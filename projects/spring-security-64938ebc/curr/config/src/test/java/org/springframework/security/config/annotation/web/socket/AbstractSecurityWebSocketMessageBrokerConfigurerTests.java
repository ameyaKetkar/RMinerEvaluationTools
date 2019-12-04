/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.security.config.annotation.web.socket;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.access.expression.SecurityExpressionOperations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.messaging.access.expression.DefaultMessageSecurityExpressionHandler;
import org.springframework.security.messaging.access.expression.MessageSecurityExpressionRoot;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Controller;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.socket.sockjs.transport.handler.SockJsWebSocketHandler;
import org.springframework.web.socket.sockjs.transport.session.WebSocketServerSockJsSession;

public class AbstractSecurityWebSocketMessageBrokerConfigurerTests {
	AnnotationConfigWebApplicationContext context;

	TestingAuthenticationToken messageUser;

	CsrfToken token;

	String sessionAttr;

	@Before
	public void setup() {
		token = new DefaultCsrfToken("header", "param", "token");
		sessionAttr = "sessionAttr";
		messageUser = new TestingAuthenticationToken("user", "pass", "ROLE_USER");
	}

	@After
	public void cleanup() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void simpleRegistryMappings() {
		loadConfig(SockJsSecurityConfig.class);

		clientInboundChannel().send(message("/permitAll"));

		try {
			clientInboundChannel().send(message("/denyAll"));
			fail("Expected Exception");
		}
		catch (MessageDeliveryException expected) {
			assertThat(expected.getCause()).isInstanceOf(AccessDeniedException.class);
		}
	}

	@Test
	public void annonymousSupported() {
		loadConfig(SockJsSecurityConfig.class);

		messageUser = null;
		clientInboundChannel().send(message("/permitAll"));
	}

	@Test
	public void addsAuthenticationPrincipalResolver() throws InterruptedException {
		loadConfig(SockJsSecurityConfig.class);

		MessageChannel messageChannel = clientInboundChannel();
		Message<String> message = message("/permitAll/authentication");
		messageChannel.send(message);

		assertThat(context.getBean(MyController.class).authenticationPrincipal)
				.isEqualTo((String) messageUser.getPrincipal());
	}

	@Test
	public void addsAuthenticationPrincipalResolverWhenNoAuthorization()
			throws InterruptedException {
		loadConfig(NoInboundSecurityConfig.class);

		MessageChannel messageChannel = clientInboundChannel();
		Message<String> message = message("/permitAll/authentication");
		messageChannel.send(message);

		assertThat(context.getBean(MyController.class).authenticationPrincipal)
				.isEqualTo((String) messageUser.getPrincipal());
	}

	@Test
	public void addsCsrfProtectionWhenNoAuthorization() throws InterruptedException {
		loadConfig(NoInboundSecurityConfig.class);

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor
				.create(SimpMessageType.CONNECT);
		Message<?> message = message(headers, "/authentication");
		MessageChannel messageChannel = clientInboundChannel();

		try {
			messageChannel.send(message);
			fail("Expected Exception");
		}
		catch (MessageDeliveryException success) {
			assertThat(success.getCause()).isInstanceOf(MissingCsrfTokenException.class);
		}
	}

	@Test
	public void csrfProtectionForConnect() throws InterruptedException {
		loadConfig(SockJsSecurityConfig.class);

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor
				.create(SimpMessageType.CONNECT);
		Message<?> message = message(headers, "/authentication");
		MessageChannel messageChannel = clientInboundChannel();

		try {
			messageChannel.send(message);
			fail("Expected Exception");
		}
		catch (MessageDeliveryException success) {
			assertThat(success.getCause()).isInstanceOf(MissingCsrfTokenException.class);
		}
	}

	@Test
	public void csrfProtectionDisabledForConnect() throws InterruptedException {
		loadConfig(CsrfDisabledSockJsSecurityConfig.class);

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor
				.create(SimpMessageType.CONNECT);
		Message<?> message = message(headers, "/permitAll/connect");
		MessageChannel messageChannel = clientInboundChannel();

		messageChannel.send(message);
	}

	@Test
	public void messagesConnectUseCsrfTokenHandshakeInterceptor() throws Exception {

		loadConfig(SockJsSecurityConfig.class);

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor
				.create(SimpMessageType.CONNECT);
		Message<?> message = message(headers, "/authentication");
		MockHttpServletRequest request = sockjsHttpRequest("/chat");
		HttpRequestHandler handler = handler(request);

		handler.handleRequest(request, new MockHttpServletResponse());

		assertHandshake(request);
	}

	@Test
	public void messagesConnectUseCsrfTokenHandshakeInterceptorMultipleMappings()
			throws Exception {
		loadConfig(SockJsSecurityConfig.class);

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor
				.create(SimpMessageType.CONNECT);
		Message<?> message = message(headers, "/authentication");
		MockHttpServletRequest request = sockjsHttpRequest("/other");
		HttpRequestHandler handler = handler(request);

		handler.handleRequest(request, new MockHttpServletResponse());

		assertHandshake(request);
	}

	@Test
	public void messagesConnectWebSocketUseCsrfTokenHandshakeInterceptor()
			throws Exception {
		loadConfig(WebSocketSecurityConfig.class);

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor
				.create(SimpMessageType.CONNECT);
		Message<?> message = message(headers, "/authentication");
		MockHttpServletRequest request = websocketHttpRequest("/websocket");
		HttpRequestHandler handler = handler(request);

		handler.handleRequest(request, new MockHttpServletResponse());

		assertHandshake(request);
	}

	@Test
	public void msmsRegistryCustomPatternMatcher()
			throws Exception {
		loadConfig(MsmsRegistryCustomPatternMatcherConfig.class);

		clientInboundChannel().send(message("/app/a.b"));

		try {
			clientInboundChannel().send(message("/app/a.b.c"));
			fail("Expected Exception");
		}
		catch (MessageDeliveryException expected) {
			assertThat(expected.getCause()).isInstanceOf(AccessDeniedException.class);
		}
	}

	@Configuration
	@EnableWebSocketMessageBroker
	@Import(SyncExecutorConfig.class)
	static class MsmsRegistryCustomPatternMatcherConfig extends
			AbstractSecurityWebSocketMessageBrokerConfigurer {

		// @formatter:off
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry
				.addEndpoint("/other")
				.setHandshakeHandler(testHandshakeHandler());
		}
		// @formatter:on

		// @formatter:off
		@Override
		protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
			messages
				.simpDestMatchers("/app/a.*").permitAll()
				.anyMessage().denyAll();
		}
		// @formatter:on

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.setPathMatcher(new AntPathMatcher("."));
			registry.enableSimpleBroker("/queue/", "/topic/");
			registry.setApplicationDestinationPrefixes("/app");
		}

		@Bean
		public TestHandshakeHandler testHandshakeHandler() {
			return new TestHandshakeHandler();
		}
	}

	@Test
	public void overrideMsmsRegistryCustomPatternMatcher()
			throws Exception {
		loadConfig(OverrideMsmsRegistryCustomPatternMatcherConfig.class);

		clientInboundChannel().send(message("/app/a/b"));

		try {
			clientInboundChannel().send(message("/app/a/b/c"));
			fail("Expected Exception");
		}
		catch (MessageDeliveryException expected) {
			assertThat(expected.getCause()).isInstanceOf(AccessDeniedException.class);
		}
	}

	@Configuration
	@EnableWebSocketMessageBroker
	@Import(SyncExecutorConfig.class)
	static class OverrideMsmsRegistryCustomPatternMatcherConfig extends
			AbstractSecurityWebSocketMessageBrokerConfigurer {

		// @formatter:off
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry
				.addEndpoint("/other")
				.setHandshakeHandler(testHandshakeHandler());
		}
		// @formatter:on


		// @formatter:off
		@Override
		protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
			messages
				.simpDestPathMatcher(new AntPathMatcher())
				.simpDestMatchers("/app/a/*").permitAll()
				.anyMessage().denyAll();
		}
		// @formatter:on

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.setPathMatcher(new AntPathMatcher("."));
			registry.enableSimpleBroker("/queue/", "/topic/");
			registry.setApplicationDestinationPrefixes("/app");
		}

		@Bean
		public TestHandshakeHandler testHandshakeHandler() {
			return new TestHandshakeHandler();
		}
	}

	@Test
	public void defaultPatternMatcher()
			throws Exception {
		loadConfig(DefaultPatternMatcherConfig.class);

		clientInboundChannel().send(message("/app/a/b"));

		try {
			clientInboundChannel().send(message("/app/a/b/c"));
			fail("Expected Exception");
		}
		catch (MessageDeliveryException expected) {
			assertThat(expected.getCause()).isInstanceOf(AccessDeniedException.class);
		}
	}

	@Configuration
	@EnableWebSocketMessageBroker
	@Import(SyncExecutorConfig.class)
	static class DefaultPatternMatcherConfig extends
			AbstractSecurityWebSocketMessageBrokerConfigurer {

		// @formatter:off
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry
				.addEndpoint("/other")
				.setHandshakeHandler(testHandshakeHandler());
		}
		// @formatter:on

		// @formatter:off
		@Override
		protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
			messages
				.simpDestMatchers("/app/a/*").permitAll()
				.anyMessage().denyAll();
		}
		// @formatter:on

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.enableSimpleBroker("/queue/", "/topic/");
			registry.setApplicationDestinationPrefixes("/app");
		}

		@Bean
		public TestHandshakeHandler testHandshakeHandler() {
			return new TestHandshakeHandler();
		}
	}

	@Test
	public void customExpression()
			throws Exception {
		loadConfig(CustomExpressionConfig.class);

		clientInboundChannel().send(message("/denyRob"));

		this.messageUser = new TestingAuthenticationToken("rob", "password", "ROLE_USER");
		try {
			clientInboundChannel().send(message("/denyRob"));
			fail("Expected Exception");
		}
		catch (MessageDeliveryException expected) {
			assertThat(expected.getCause()).isInstanceOf(AccessDeniedException.class);
		}
	}

	@Configuration
	@EnableWebSocketMessageBroker
	@Import(SyncExecutorConfig.class)
	static class CustomExpressionConfig extends
			AbstractSecurityWebSocketMessageBrokerConfigurer {

		// @formatter:off
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry
				.addEndpoint("/other")
				.setHandshakeHandler(testHandshakeHandler());
		}
		// @formatter:on

		// @formatter:off
		@Override
		protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
			messages
				.anyMessage().access("denyRob()");
		}
		// @formatter:on

		@Bean
		public SecurityExpressionHandler<Message<Object>> messageSecurityExpressionHandler() {
			return new DefaultMessageSecurityExpressionHandler<Object>() {
				@Override
				protected SecurityExpressionOperations createSecurityExpressionRoot(
						Authentication authentication,
						Message<Object> invocation) {
					return new MessageSecurityExpressionRoot(authentication, invocation) {
						public boolean denyRob() {
							Authentication auth = getAuthentication();
							return auth != null && !"rob".equals(auth.getName());
						}
					};
				}
			};
		}

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.enableSimpleBroker("/queue/", "/topic/");
			registry.setApplicationDestinationPrefixes("/app");
		}

		@Bean
		public TestHandshakeHandler testHandshakeHandler() {
			return new TestHandshakeHandler();
		}
	}

	private void assertHandshake(HttpServletRequest request) {
		TestHandshakeHandler handshakeHandler = context
				.getBean(TestHandshakeHandler.class);
		assertThat(handshakeHandler.attributes.get(CsrfToken.class.getName())).isSameAs(
				token);
		assertThat(handshakeHandler.attributes.get(sessionAttr)).isEqualTo(
				request.getSession().getAttribute(sessionAttr));
	}

	private HttpRequestHandler handler(HttpServletRequest request) throws Exception {
		HandlerMapping handlerMapping = context.getBean(HandlerMapping.class);
		return (HttpRequestHandler) handlerMapping.getHandler(request).getHandler();
	}

	private MockHttpServletRequest websocketHttpRequest(String mapping) {
		MockHttpServletRequest request = sockjsHttpRequest(mapping);
		request.setRequestURI(mapping);
		return request;
	}

	private MockHttpServletRequest sockjsHttpRequest(String mapping) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE,
				"/289/tpyx6mde/websocket");
		request.setRequestURI(mapping + "/289/tpyx6mde/websocket");
		request.getSession().setAttribute(sessionAttr, "sessionValue");

		request.setAttribute(CsrfToken.class.getName(), token);
		return request;
	}

	private Message<String> message(String destination) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
		return message(headers, destination);
	}

	private Message<String> message(SimpMessageHeaderAccessor headers, String destination) {
		headers.setSessionId("123");
		headers.setSessionAttributes(new HashMap<String, Object>());
		if (destination != null) {
			headers.setDestination(destination);
		}
		if (messageUser != null) {
			headers.setUser(messageUser);
		}
		return new GenericMessage<String>("hi", headers.getMessageHeaders());
	}

	private MessageChannel clientInboundChannel() {
		return context.getBean("clientInboundChannel", MessageChannel.class);
	}

	private void loadConfig(Class<?>... configs) {
		context = new AnnotationConfigWebApplicationContext();
		context.register(configs);
		context.setServletConfig(new MockServletConfig());
		context.refresh();
	}

	@Controller
	static class MyController {

		String authenticationPrincipal;
		MyCustomArgument myCustomArgument;

		@MessageMapping("/authentication")
		public void authentication(@AuthenticationPrincipal String un) {
			this.authenticationPrincipal = un;
		}

		@MessageMapping("/myCustom")
		public void myCustom(MyCustomArgument myCustomArgument) {
			this.myCustomArgument = myCustomArgument;
		}
	}

	static class MyCustomArgument {
		MyCustomArgument(String notDefaultConstr) {
		}
	}

	static class MyCustomArgumentResolver implements HandlerMethodArgumentResolver {

		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.getParameterType().isAssignableFrom(MyCustomArgument.class);
		}

		public Object resolveArgument(MethodParameter parameter, Message<?> message)
				throws Exception {
			return new MyCustomArgument("");
		}
	}

	static class TestHandshakeHandler implements HandshakeHandler {
		Map<String, Object> attributes;

		public boolean doHandshake(ServerHttpRequest request,
				ServerHttpResponse response, WebSocketHandler wsHandler,
				Map<String, Object> attributes) throws HandshakeFailureException {
			this.attributes = attributes;
			if (wsHandler instanceof SockJsWebSocketHandler) {
				// work around SPR-12716
				SockJsWebSocketHandler sockJs = (SockJsWebSocketHandler) wsHandler;
				WebSocketServerSockJsSession session = (WebSocketServerSockJsSession) ReflectionTestUtils
						.getField(sockJs, "sockJsSession");
				this.attributes = session.getAttributes();
			}
			return true;
		}
	}

	@Configuration
	@EnableWebSocketMessageBroker
	@Import(SyncExecutorConfig.class)
	static class SockJsSecurityConfig extends
			AbstractSecurityWebSocketMessageBrokerConfigurer {

		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/other").setHandshakeHandler(testHandshakeHandler())
					.withSockJS().setInterceptors(new HttpSessionHandshakeInterceptor());

			registry.addEndpoint("/chat").setHandshakeHandler(testHandshakeHandler())
					.withSockJS().setInterceptors(new HttpSessionHandshakeInterceptor());
		}

		// @formatter:off
		@Override
		protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
			messages
				.simpDestMatchers("/permitAll/**").permitAll()
				.anyMessage().denyAll();
		}
		// @formatter:on

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.enableSimpleBroker("/queue/", "/topic/");
			registry.setApplicationDestinationPrefixes("/permitAll", "/denyAll");
		}

		@Bean
		public MyController myController() {
			return new MyController();
		}

		@Bean
		public TestHandshakeHandler testHandshakeHandler() {
			return new TestHandshakeHandler();
		}
	}

	@Configuration
	@EnableWebSocketMessageBroker
	@Import(SyncExecutorConfig.class)
	static class NoInboundSecurityConfig extends
			AbstractSecurityWebSocketMessageBrokerConfigurer {

		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/other").withSockJS()
					.setInterceptors(new HttpSessionHandshakeInterceptor());

			registry.addEndpoint("/chat").withSockJS()
					.setInterceptors(new HttpSessionHandshakeInterceptor());
		}

		@Override
		protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
		}

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.enableSimpleBroker("/queue/", "/topic/");
			registry.setApplicationDestinationPrefixes("/permitAll", "/denyAll");
		}

		@Bean
		public MyController myController() {
			return new MyController();
		}
	}

	@Configuration
	static class CsrfDisabledSockJsSecurityConfig extends SockJsSecurityConfig {

		@Override
		protected boolean sameOriginDisabled() {
			return true;
		}
	}

	@Configuration
	@EnableWebSocketMessageBroker
	@Import(SyncExecutorConfig.class)
	static class WebSocketSecurityConfig extends
			AbstractSecurityWebSocketMessageBrokerConfigurer {

		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/websocket")
					.setHandshakeHandler(testHandshakeHandler())
					.addInterceptors(new HttpSessionHandshakeInterceptor());
		}

		// @formatter:off
		@Override
		protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
			messages
				.simpDestMatchers("/permitAll/**").permitAll()
				.simpDestMatchers("/customExpression/**").access("denyRob")
				.anyMessage().denyAll();
		}
		// @formatter:on

		@Bean
		public TestHandshakeHandler testHandshakeHandler() {
			return new TestHandshakeHandler();
		}
	}

	@Configuration
	static class SyncExecutorConfig {
		@Bean
		public static SyncExecutorSubscribableChannelPostProcessor postProcessor() {
			return new SyncExecutorSubscribableChannelPostProcessor();
		}
	}
}