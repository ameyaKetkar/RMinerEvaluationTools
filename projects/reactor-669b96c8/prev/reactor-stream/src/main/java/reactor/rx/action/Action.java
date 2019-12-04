/*
 * Copyright (c) 2011-2014 Pivotal Software, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package reactor.rx.action;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.Environment;
import reactor.core.Dispatcher;
import reactor.core.alloc.Recyclable;
import reactor.core.dispatch.SynchronousDispatcher;
import reactor.core.dispatch.TailRecurseDispatcher;
import reactor.core.processor.CancelException;
import reactor.core.queue.CompletableLinkedQueue;
import reactor.core.queue.CompletableQueue;
import reactor.core.support.Exceptions;
import reactor.core.support.NonBlocking;
import reactor.core.support.SpecificationExceptions;
import reactor.fn.Consumer;
import reactor.fn.Supplier;
import reactor.fn.tuple.Tuple;
import reactor.fn.tuple.Tuple2;
import reactor.rx.Stream;
import reactor.rx.StreamUtils;
import reactor.rx.action.combination.FanInAction;
import reactor.rx.broadcast.Broadcaster;
import reactor.rx.subscription.DropSubscription;
import reactor.rx.subscription.FanOutSubscription;
import reactor.rx.subscription.PushSubscription;
import reactor.rx.subscription.ReactiveSubscription;

/**
 * An Action is a reactive component to subscribe to a {@link org.reactivestreams.Publisher} and in particular
 * to a {@link reactor.rx.Stream}. Stream is usually the place where actions are created.
 * <p>
 * An Action is also a data producer, and therefore implements {@link org.reactivestreams.Processor}.
 * An imperative programming equivalent of an action is a method or function. The main difference is that it also
 * reacts on various {@link org.reactivestreams.Subscriber} signals and produce an output data {@param O} for
 * any downstream subscription.
 * <p>
 * The implementation specifics of an Action lies in two core features:
 * - Its signal scheduler on {@link reactor.core.Dispatcher}
 * - Its smart capacity awareness to prevent {@link reactor.core.Dispatcher} overflow
 * <p>
 * Up to a maximum capacity defined with {@link this#capacity(long)} will be allowed to be dispatched by requesting
 * the tracked remaining slots to the upstream {@link org.reactivestreams.Subscription}. This maximum in-flight data
 * is a value to tune accordingly with the system and the requirements. An Action will bypass this feature anytime it is
 * not the root of stream processing chain e.g.:
 * <p>
 * stream.filter(..).map(..) :
 * <p>
 * In that Stream, filter is a FilterAction and has no upstream action, only the publisher it is attached to.
 * The FilterAction will decide to be capacity aware and will track demand.
 * The MapAction will however behave like a firehose and will not track the demand, passing any request upstream.
 * <p>
 * Implementing an Action is highly recommended to work with Stream without dealing with tracking issues and other
 * threading matters. Usually an implementation will override any doXXXXX method where 'do' is an hint that logic will
 * safely be dispatched to avoid race-conditions.
 *
 * @param <I> The input {@link this#onNext(Object)} signal
 * @param <O> The output type to listen for with {@link this#subscribe(org.reactivestreams.Subscriber)}
 * @author Stephane Maldini
 * @since 1.1, 2.0
 */
public abstract class Action<I, O> extends Stream<O>
		implements Processor<I, O>, Consumer<I>, Recyclable, Control {

	/**
	 * onComplete, onError, request, onSubscribe are dispatched events, therefore up to capacity + 4 events can be
	 * in-flight
	 * stacking into a Dispatcher.
	 */
	public static final int RESERVED_SLOTS = 4;
	public static final int NO_CAPACITY    = -1;

	/**
	 * The upstream request tracker to avoid dispatcher overrun, based on the current {@link this#capacity}
	 */
	protected PushSubscription<I> upstreamSubscription;
	protected PushSubscription<O> downstreamSubscription;

	protected long capacity;

	public static void checkRequest(long n) {
		if (n <= 0l) {
			throw SpecificationExceptions.spec_3_09_exception(n);
		}
	}

	public static long evaluateCapacity(long n) {
		return n != Long.MAX_VALUE ?
				Math.max(Action.RESERVED_SLOTS, n - Action.RESERVED_SLOTS) :
				Long.MAX_VALUE;
	}

	public Action() {
		this(Long.MAX_VALUE);
	}

	public Action(long batchSize) {
		this.capacity = batchSize;
	}

	/**
	 * --------------------------------------------------------------------------------------------------------
	 * ACTION SIGNAL HANDLING
	 * --------------------------------------------------------------------------------------------------------
	 */


	@Override
	public void subscribe(final Subscriber<? super O> subscriber) {
		try {
			final NonBlocking asyncSubscriber = NonBlocking.class.isAssignableFrom(subscriber.getClass()) ?
					(NonBlocking) subscriber :
					null;

			boolean isReactiveCapacity = null == asyncSubscriber || asyncSubscriber.isReactivePull(getDispatcher(),
					capacity);

			final PushSubscription<O> subscription = createSubscription(subscriber,
					isReactiveCapacity);

			if (subscription == null)
				return;

			if (null != asyncSubscriber && isReactiveCapacity) {
				subscription.maxCapacity(asyncSubscriber.getCapacity());
			}

			subscribeWithSubscription(subscriber, subscription);

		}catch (Throwable throwable){
			Exceptions.throwIfFatal(throwable);
			subscriber.onError(throwable);
		}
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		if (subscription == null) {
			throw new NullPointerException("Spec 2.13: Subscription cannot be null");
		}

		final boolean hasRequestTracker = upstreamSubscription != null;

		//if request tracker was connected to another subscription
		if (hasRequestTracker) {
			subscription.cancel();
			return;
		}

		upstreamSubscription = createTrackingSubscription(subscription);
		upstreamSubscription.maxCapacity(getCapacity());

		try {
			doOnSubscribe(subscription);
			doStart();
		} catch (Throwable t) {
			Exceptions.throwIfFatal(t);
			doError(t);
		}
	}

	protected final void doStart() {
		final PushSubscription<O> downSub = downstreamSubscription;
		if (downSub != null) {
				downSub.start();
		}
	}

	@Override
	public final void accept(I i) {
		onNext(i);
	}

	@Override
	public void onNext(I ev) {
		if (ev == null) {
			throw new NullPointerException("Spec 2.13: Signal cannot be null");
		}

		if (upstreamSubscription == null && downstreamSubscription == null) {
			throw CancelException.get();
		}

		try {
			doNext(ev);
		} catch (CancelException uae){
			throw uae;
		} catch (Throwable cause) {
			doError(Exceptions.addValueAsLastCause(cause, ev));
		}
	}

	@Override
	public void onComplete() {
		try {
			doComplete();
			doShutdown();
		} catch (Throwable t) {
			doError(t);
		}
	}

	@Override
	public void onError(Throwable cause) {
		if (cause == null) {
			throw new NullPointerException("Spec 2.13: Signal cannot be null");
		}
		if (upstreamSubscription != null) upstreamSubscription.updatePendingRequests(0l);
		doError(cause);
		doShutdown();
	}

	/**
	 * --------------------------------------------------------------------------------------------------------
	 * ACTION MODIFIERS
	 * --------------------------------------------------------------------------------------------------------
	 */

	@Override
	public Action<I, O> capacity(long elements) {
		Dispatcher dispatcher = getDispatcher();
		if (dispatcher != SynchronousDispatcher.INSTANCE && dispatcher.getClass() != TailRecurseDispatcher.class) {
			long dispatcherCapacity = evaluateCapacity(dispatcher.backlogSize());
			capacity = elements > dispatcherCapacity ? dispatcherCapacity : elements;
		} else {
			capacity = elements;
		}

		if (upstreamSubscription != null) {
			upstreamSubscription.maxCapacity(capacity);
		}
		return this;
	}

	/**
	 * Send an element of parameterized type {link O} to all the attached {@link Subscriber}.
	 * A Stream must be in READY state to dispatch signals and will fail fast otherwise (IllegalStateException).
	 *
	 * @param ev the data to forward
	 * @since 2.0
	 */
	protected void broadcastNext(final O ev) {
		//log.debug("event [" + ev + "] by: " + getClass().getSimpleName());
		PushSubscription<O> downstreamSubscription = this.downstreamSubscription;
		if (downstreamSubscription == null) {
				throw CancelException.get();
		}

		try {
			downstreamSubscription.onNext(ev);
		} catch(CancelException ce){
			throw ce;
		} catch (Throwable throwable) {
			doError(Exceptions.addValueAsLastCause(throwable, ev));
		}
	}

	/**
	 * Send an error to all the attached {@link Subscriber}.
	 * A Stream must be in READY state to dispatch signals and will fail fast otherwise (IllegalStateException).
	 *
	 * @param throwable the error to forward
	 * @since 2.0
	 */
	protected void broadcastError(final Throwable throwable) {
		//log.debug("event [" + throwable + "] by: " + getClass().getSimpleName());
		/*if (!isRunning()) {
			if (log.isTraceEnabled()) {
				log.trace("error dropped by: " + getClass().getSimpleName() + ":" + this, throwable);
			}
		}*/

		if (downstreamSubscription == null) {
			if (Environment.alive()) {
				Environment.get().routeError(throwable);
			}
			return;
		}

		downstreamSubscription.onError(throwable);
	}

	/**
	 * Send a complete event to all the attached {@link Subscriber} ONLY IF the underlying state is READY.
	 * Unlike {@link #broadcastNext(Object)} and {@link #broadcastError(Throwable)} it will simply ignore the signal.
	 *
	 * @since 2.0
	 */
	protected void broadcastComplete() {
		//log.debug("event [complete] by: " + getClass().getSimpleName());
		if (downstreamSubscription == null) {
			return;
		}

		try {
			downstreamSubscription.onComplete();
		} catch (Throwable throwable) {
			doError(throwable);
		}
	}

	@Override
	public boolean isPublishing() {
		PushSubscription<I> parentSubscription = upstreamSubscription;
		return parentSubscription != null && !parentSubscription.isComplete();
	}


	public void cancel() {
		PushSubscription<I> parentSub = upstreamSubscription;
		if (parentSub != null) {
			upstreamSubscription = null;
			parentSub.cancel();
		}
	}

	@Override
	public void requestAll() {
		if (downstreamSubscription == null) {
			requestMore(Long.MAX_VALUE);
		}
	}

	/**
	 * Print a debugged form of the root action relative to this one. The output will be an acyclic directed graph of
	 * composed actions.
	 *
	 * @since 2.0
	 */
	@SuppressWarnings("unchecked")
	public StreamUtils.StreamVisitor debug() {
		return StreamUtils.browse(findOldestUpstream(Action.class));
	}

	/**
	 * --------------------------------------------------------------------------------------------------------
	 * STREAM ACTION-SPECIFIC EXTENSIONS
	 * --------------------------------------------------------------------------------------------------------
	 */

	/**
	 * Consume a Stream to allow for dynamic {@link Action} update. Everytime
	 * the {@param controlStream} receives a next signal, the current Action and the input data will be published as a
	 * {@link reactor.fn.tuple.Tuple2} to the attached {@param controller}.
	 * <p>
	 * This is particulary useful to dynamically adapt the {@link Stream} instance : capacity(), pause(), resume()...
	 *
	 * @param controlStream The consumed stream, each signal will trigger the passed controller
	 * @param controller    The consumer accepting a pair of Stream and user-provided signal type
	 * @return the current {@link Stream} instance
	 * @since 2.0
	 */
	public final <E> Action<I, O> control(Stream<E> controlStream, final Consumer<Tuple2<Action<I, O>,
			? super E>> controller) {
		final Action<I, O> thiz = this;
		controlStream.consume(new Consumer<E>() {
			@Override
			public void accept(E e) {
				controller.accept(Tuple.of(thiz, e));
			}
		});
		return this;
	}

	@Override
	public final Stream<O> onOverflowBuffer(final Supplier<? extends CompletableQueue<O>> queueSupplier) {
		return lift(new Supplier<Action<O, O>>() {
			@Override
			public Action<O, O> get() {
				Broadcaster<O> newStream = Broadcaster.<O>create(getEnvironment(), getDispatcher()).capacity(capacity);
				if (queueSupplier == null) {
					subscribeWithSubscription(newStream, new DropSubscription<O>(Action.this, newStream) {
						@Override
						public void request(long elements) {
							super.request(elements);
							requestUpstream(capacity, isComplete(), elements);
						}
					});
				} else {
					subscribeWithSubscription(newStream,
							createSubscription(newStream, queueSupplier.get()));
				}
				return newStream;
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public final <E> CompositeAction<E, O> combine() {
		final Action<E, ?> subscriber = (Action<E, ?>) findOldestUpstream(Action.class);
		subscriber.upstreamSubscription = null;
		return new CompositeAction<E, O>(subscriber, this);
	}

	/**
	 * Create a consumer that broadcast complete signal from any accepted value.
	 *
	 * @return a new {@link Consumer} ready to forward complete signal to this stream
	 * @since 2.0
	 */
	public final Consumer<?> toBroadcastCompleteConsumer() {
		return new Consumer<Object>() {
			@Override
			public void accept(Object o) {
				broadcastComplete();
			}
		};
	}


	/**
	 * Create a consumer that broadcast next signal from accepted values.
	 *
	 * @return a new {@link Consumer} ready to forward values to this stream
	 * @since 2.0
	 */
	public final Consumer<O> toBroadcastNextConsumer() {
		return new Consumer<O>() {
			@Override
			public void accept(O o) {
				broadcastNext(o);
			}
		};
	}

	/**
	 * Create a consumer that broadcast error signal from any accepted value.
	 *
	 * @return a new {@link Consumer} ready to forward error to this stream
	 * @since 2.0
	 */
	public final Consumer<Throwable> toBroadcastErrorConsumer() {
		return new Consumer<Throwable>() {
			@Override
			public void accept(Throwable o) {
				broadcastError(o);
			}
		};
	}

	/**
	 * Utility to find the most ancient subscribed Action.
	 * Also used by debug() operation to render the complete flow from upstream.
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <P extends Publisher<?>> P findOldestUpstream(Class<P> clazz) {
		Action<?, ?> that = this;

		while (inspectPublisher(that, Action.class)) {

			that = (Action<?, ?>) that.upstreamSubscription.getPublisher();

			if (that != null) {

				if (FanInAction.class.isAssignableFrom(that.getClass())) {
					that = ((FanInAction) that).dynamicMergeAction() != null ? ((FanInAction) that).dynamicMergeAction() : that;
				}
			}
		}

		if (inspectPublisher(that, clazz)) {
			return (P) ((PushSubscription<?>) that.upstreamSubscription).getPublisher();
		} else {
			return (P) that;
		}
	}

	/**
	 * --------------------------------------------------------------------------------------------------------
	 * ACTION STATE
	 * --------------------------------------------------------------------------------------------------------
	 */

	@Override
	public final long getCapacity() {
		return capacity;
	}

	/**
	 * Get the current upstream subscription if any
	 *
	 * @return current {@link org.reactivestreams.Subscription}
	 */
	public PushSubscription<I> getSubscription() {
		return upstreamSubscription;
	}


	/**
	 * Get the current action child subscription
	 *
	 * @return current child {@link reactor.rx.subscription.PushSubscription}
	 */
	public final PushSubscription<O> downstreamSubscription() {
		return downstreamSubscription;
	}

	/**
	 * --------------------------------------------------------------------------------------------------------
	 * INTERNALS
	 * --------------------------------------------------------------------------------------------------------
	 */

	@Override
	public boolean cancelSubscription(final PushSubscription<O> subscription) {
		if (this.downstreamSubscription == null) return false;

		if (subscription == this.downstreamSubscription) {
			this.downstreamSubscription = null;
			cancel();
			return true;
		} else {
			PushSubscription<O> dsub = this.downstreamSubscription;
			if (FanOutSubscription.class.isAssignableFrom(dsub.getClass())) {
				FanOutSubscription<O> fsub =
						((FanOutSubscription<O>) this.downstreamSubscription);

				if (fsub.remove(subscription) && fsub.isEmpty()) {
					cancel();
					return true;
				}
			}
			return false;
		}
	}

	protected PushSubscription<O> createSubscription(final Subscriber<? super O> subscriber, boolean reactivePull) {
		return createSubscription(subscriber, reactivePull ? new CompletableLinkedQueue<O>() : null);
	}

	protected PushSubscription<O> createSubscription(final Subscriber<? super O> subscriber, CompletableQueue<O> queue) {
		if (queue != null) {
			return new ReactiveSubscription<O>(this, subscriber, queue) {

				@Override
				protected void onRequest(long elements) {
					requestUpstream(capacity, buffer.isComplete(), elements);
				}
			};
		} else {
			return new PushSubscription<O>(this, subscriber) {
				@Override
				protected void onRequest(long elements) {
					requestUpstream(NO_CAPACITY, isComplete(), elements);
				}
			};
		}
	}

	protected void requestUpstream(long capacity, boolean terminated, long elements) {
		if (upstreamSubscription != null && !terminated) {
			requestMore(elements);
		} else {
			PushSubscription<O> _downstreamSubscription = downstreamSubscription;
			if (_downstreamSubscription != null) {
				_downstreamSubscription.updatePendingRequests(elements);
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected PushSubscription<I> createTrackingSubscription(Subscription subscription) {
		//If not a reactor push subscription, wrap within one
		if (!PushSubscription.class.isAssignableFrom(subscription.getClass())) {
			return PushSubscription.wrap(subscription, this);
		} else {
			return ((PushSubscription<I>) subscription);
		}
	}

	protected void doOnSubscribe(Subscription subscription) {
	}

	protected void doComplete() {
		broadcastComplete();
	}

	abstract protected void doNext(I ev);

	protected void doError(Throwable ev) {
		if (downstreamSubscription != null) {
			try {
				downstreamSubscription.onError(ev);
				return;
			} catch (Throwable t) {
				Environment.get().routeError(t);
			}
		}

		if (Environment.alive()) {
			Environment.get().routeError(ev);
		}
	}

	@Override
	public void requestMore(final long n) {
		checkRequest(n);
		if (upstreamSubscription != null) {
			upstreamSubscription.request(n);
		}
	}

	/**
	 * Subscribe a given subscriber and pairs it with a given subscription instead of letting the Stream pick it
	 * automatically.
	 * <p>
	 * This is mainly useful for libraries implementors, usually {@link this#lift(reactor.fn.Supplier)} and
	 * {@link this#subscribe(org.reactivestreams.Subscriber)} are just fine.
	 *
	 * @param subscriber
	 * @param subscription
	 */
	protected void subscribeWithSubscription(final Subscriber<? super O> subscriber, final PushSubscription<O>
			subscription) {
		try {
			if (!addSubscription(subscription)) {
				subscriber.onError(new IllegalStateException("The subscription cannot be linked to this Stream"));
			} else {
				subscription.markAsDeferredStart();
				if (upstreamSubscription != null) {
					subscription.start();
				}
			}
		} catch (Exception e) {
			Exceptions.throwIfFatal(e);
			subscriber.onError(e);
		}
	}


	@SuppressWarnings("unchecked")
	protected boolean addSubscription(final PushSubscription<O> subscription) {
		PushSubscription<O> currentSubscription = this.downstreamSubscription;
		if (currentSubscription == null) {
			this.downstreamSubscription = subscription;
			return true;
		} else if (currentSubscription.equals(subscription)) {
			subscription.onError(SpecificationExceptions.spec_2_12_exception());
			return false;
		} else if (FanOutSubscription.class.isAssignableFrom(currentSubscription.getClass())) {
			if (((FanOutSubscription<O>) currentSubscription).contains(subscription)) {
				subscription.onError(SpecificationExceptions.spec_2_12_exception());
				return false;
			} else {
				return ((FanOutSubscription<O>) currentSubscription).add(subscription);
			}
		} else {
			this.downstreamSubscription = new FanOutSubscription<O>(this, currentSubscription, subscription);
			return true;
		}
	}

	protected void doShutdown() {
		//recycle();
	}

	private boolean inspectPublisher(Action<?, ?> that, Class<?> actionClass) {
		return that.upstreamSubscription != null
				&& ((PushSubscription<?>) that.upstreamSubscription).getPublisher() != null
				&& actionClass.isAssignableFrom(((PushSubscription<?>) that.upstreamSubscription).getPublisher().getClass());
	}

	@Override
	public void recycle() {
		downstreamSubscription = null;
		upstreamSubscription = null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public String toString() {
		return "{" +
				(capacity != Long.MAX_VALUE || upstreamSubscription == null ?
						"{dispatcher=" + getDispatcher() +
								((!SynchronousDispatcher.class.isAssignableFrom(getDispatcher().getClass()) ? (":" + getDispatcher()
										.remainingSlots()) :
										"")) +
								", max-capacity=" + (capacity == Long.MAX_VALUE ? "infinite" : capacity) + "}"
						: "") +
				(upstreamSubscription != null ? upstreamSubscription : "") + '}';
	}

}
