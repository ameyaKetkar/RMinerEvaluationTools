package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.auxiliary.TypeProxy;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

/**
 * Parameters that are annotated with this annotation are assigned an instance of an auxiliary proxy type that allows calling
 * any {@code super} methods of the instrumented type where the parameter type must be a super type of the instrumented type.
 * The proxy type will be a direct subclass of the parameter's type such as for example a specific interface.
 * <p>&nbsp;</p>
 * Obviously, the proxy type must be instantiated before it is assigned to the intercepting method's parameter. For this
 * purpose, two strategies are available which can be specified by setting the {@link Super#strategy()} parameter which can
 * be assigned:
 * <ol>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.Super.Instantiation#CONSTRUCTOR}:
 * A constructor call is made where {@link Super#constructorParameters()} determines the constructor's signature. Any constructor
 * parameter is assigned the parameter's default value when the constructor is called. Calling the default constructor is the
 * preconfigured strategy.</li>
 * <li>{@link net.bytebuddy.implementation.bind.annotation.Super.Instantiation#UNSAFE}:
 * The proxy is created by making use of Java's {@link sun.reflect.ReflectionFactory} which is however not a public API which
 * is why it should be used with care. No constructor is called when this strategy is used. If this option is set, the
 * {@link Super#constructorParameters()} parameter is ignored.</li>
 * </ol>
 * Note that when for example intercepting a type {@code Foo} that implements some interface {@code Bar}, the proxy type
 * will only implement {@code Bar} and therefore extend {@link java.lang.Object} what allows for calling the default
 * constructor on the proxy. This implies that an interception by some method {@code qux(@Super Baz baz, @Super Bar bar)}
 * would cause the creation of two super call proxies, one extending {@code Baz}, the other extending {@code Bar}, give
 * that both types are super types of {@code Foo}.
 * <p>&nbsp;</p>
 * As an exception, no method calls to {@link Object#finalize()} are delegated by calling this method on the {@code super}-call
 * proxy by default. If this is absolutely necessary, this can however be enabled by setting {@link Super#ignoreFinalizer()}
 * to {@code false}.
 * <p>&nbsp;</p>
 * If a method parameter is not a super type of the instrumented type, the method with the parameter that is annoted by
 * #{@code Super} is not considered a possible delegation target.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Super {

    /**
     * Determines how the {@code super}call proxy type is instantiated.
     *
     * @return The instantiation strategy for this proxy.
     */
    Instantiation strategy() default Instantiation.CONSTRUCTOR;

    /**
     * If {@code true}, the proxy type will not implement {@code super} calls to {@link Object#finalize()} or any overridden methods.
     *
     * @return {@code false} if finalizer methods should be considered for {@code super}-call proxy type delegation.
     */
    boolean ignoreFinalizer() default true;

    /**
     * Determines if the generated proxy should be {@link java.io.Serializable}. If the annotated type
     * already is serializable, such an explicit specification is not required.
     *
     * @return {@code true} if the generated proxy should be {@link java.io.Serializable}.
     */
    boolean serializableProxy() default false;

    /**
     * Defines the parameter types of the constructor to be called for the created {@code super}-call proxy type.
     *
     * @return The parameter types of the constructor to be called.
     */
    Class<?>[] constructorParameters() default {};

    /**
     * Determines the instantiation of the proxy type.
     *
     * @see net.bytebuddy.implementation.bind.annotation.Super
     */
    enum Instantiation {

        /**
         * A proxy instance is instantiated by its constructor. For the constructor's arguments, the parameters default
         * values are used. The constructor can be identified by setting {@link Super#constructorParameters()}.
         */
        CONSTRUCTOR {
            @Override
            protected StackManipulation proxyFor(TypeDescription parameterType,
                                                 Implementation.Target implementationTarget,
                                                 AnnotationDescription.Loadable<Super> annotation) {
                TypeDescription[] constructorParameters = annotation.getValue(CONSTRUCTOR_PARAMETERS, TypeDescription[].class);
                List<TypeDescription> typeDescriptions = TargetType.resolve(Arrays.asList(constructorParameters),
                        implementationTarget.getTypeDescription()).asRawTypes();
                return new TypeProxy.ForSuperMethodByConstructor(parameterType,
                        implementationTarget,
                        typeDescriptions,
                        annotation.getValue(IGNORE_FINALIZER, Boolean.class),
                        annotation.getValue(SERIALIZABLE_PROXY, Boolean.class));
            }
        },

        /**
         * A proxy is instantiated by calling JVM internal methods and without calling a constructor. This strategy
         * might fail on exotic JVM implementations.
         */
        UNSAFE {
            @Override
            protected StackManipulation proxyFor(TypeDescription parameterType,
                                                 Implementation.Target implementationTarget,
                                                 AnnotationDescription.Loadable<Super> annotation) {
                return new TypeProxy.ForSuperMethodByReflectionFactory(parameterType,
                        implementationTarget,
                        annotation.getValue(IGNORE_FINALIZER, Boolean.class),
                        annotation.getValue(SERIALIZABLE_PROXY, Boolean.class));
            }
        };

        /**
         * A reference to the ignore finalizer method.
         */
        private static final MethodDescription IGNORE_FINALIZER;

        /**
         * A reference to the serializable proxy method.
         */
        private static final MethodDescription SERIALIZABLE_PROXY;

        /**
         * A reference to the constructor parameters method.
         */
        private static final MethodDescription CONSTRUCTOR_PARAMETERS;

        /*
         * Extracts method references to the annotation methods.
         */
        static {
            MethodList annotationProperties = new TypeDescription.ForLoadedType(Super.class).getDeclaredMethods();
            IGNORE_FINALIZER = annotationProperties.filter(named("ignoreFinalizer")).getOnly();
            SERIALIZABLE_PROXY = annotationProperties.filter(named("serializableProxy")).getOnly();
            CONSTRUCTOR_PARAMETERS = annotationProperties.filter(named("constructorParameters")).getOnly();
        }

        /**
         * Creates a stack manipulation which loads a {@code super}-call proxy onto the stack.
         *
         * @param parameterType        The type of the parameter that was annotated with
         *                             {@link net.bytebuddy.implementation.bind.annotation.Super}
         * @param implementationTarget The implementation target for the currently created type.
         * @param annotation           The annotation that caused this method call.
         * @return A stack manipulation representing this instance's instantiation strategy.
         */
        protected abstract StackManipulation proxyFor(TypeDescription parameterType,
                                                      Implementation.Target implementationTarget,
                                                      AnnotationDescription.Loadable<Super> annotation);

        @Override
        public String toString() {
            return "Super.Instantiation." + name();
        }
    }

    /**
     * A binder for handling the
     * {@link net.bytebuddy.implementation.bind.annotation.Super}
     * annotation.
     *
     * @see TargetMethodAnnotationDrivenBinder
     */
    enum Binder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<Super> {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * A method reference to the strategy property.
         */
        private static final MethodDescription STRATEGY;

        /*
         * Extracts method references of the super annotation.
         */
        static {
            MethodList annotationProperties = new TypeDescription.ForLoadedType(Super.class).getDeclaredMethods();
            STRATEGY = annotationProperties.filter(returns(Instantiation.class)).getOnly();
        }

        @Override
        public Class<Super> getHandledType() {
            return Super.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<Super> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner) {
            if (source.isStatic() || !implementationTarget.getTypeDescription().isAssignableTo(target.getType())) {
                return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
            } else {
                return new MethodDelegationBinder.ParameterBinding.Anonymous(annotation
                        .getValue(STRATEGY, EnumerationDescription.class).load(Instantiation.class)
                        .proxyFor(target.getType(), implementationTarget, annotation));
            }
        }

        @Override
        public String toString() {
            return "Super.Binder." + name();
        }
    }
}
