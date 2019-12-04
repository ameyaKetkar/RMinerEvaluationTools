/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration.ManagementContextResolver;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.security.AuthenticationManagerConfiguration;
import org.springframework.boot.autoconfigure.security.FallbackWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityPrerequisite;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.SpringBootWebSecurityConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity.IgnoredRequestConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for security of framework endpoints.
 * Many aspects of the behavior can be controller with {@link ManagementServerProperties}
 * via externalized application properties (or via an bean definition of that type to set
 * the defaults).
 *
 * <p>
 * The framework {@link Endpoint}s (used to expose application information to operations)
 * include a {@link Endpoint#isSensitive() sensitive} configuration option which will be
 * used as a security hint by the filter created here.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass({ EnableWebSecurity.class })
@AutoConfigureAfter(SecurityAutoConfiguration.class)
@AutoConfigureBefore(FallbackWebSecurityAutoConfiguration.class)
@EnableConfigurationProperties
public class ManagementSecurityAutoConfiguration {

	private static final String[] NO_PATHS = new String[0];

	@Bean
	@ConditionalOnMissingBean({ IgnoredPathsWebSecurityConfigurerAdapter.class })
	public IgnoredPathsWebSecurityConfigurerAdapter ignoredPathsWebSecurityConfigurerAdapter() {
		return new IgnoredPathsWebSecurityConfigurerAdapter();
	}

	@Configuration
	protected static class ManagementSecurityPropertiesConfiguration implements
			SecurityPrerequisite {

		@Autowired(required = false)
		private SecurityProperties security;

		@Autowired(required = false)
		private ManagementServerProperties management;

		@PostConstruct
		public void init() {
			if (this.management != null && this.security != null) {
				this.security.getUser().getRole()
						.add(this.management.getSecurity().getRole());
			}
		}

	}

	// Get the ignored paths in early
	@Order(SecurityProperties.IGNORED_ORDER + 1)
	private static class IgnoredPathsWebSecurityConfigurerAdapter implements
			WebSecurityConfigurer<WebSecurity> {

		@Autowired(required = false)
		private ErrorController errorController;

		@Autowired(required = false)
		private EndpointHandlerMapping endpointHandlerMapping;

		@Autowired
		private ManagementServerProperties management;

		@Autowired
		private SecurityProperties security;

		@Autowired(required = false)
		private ServerProperties server;

		@Override
		public void configure(WebSecurity builder) throws Exception {
		}

		@Override
		public void init(WebSecurity builder) throws Exception {
			IgnoredRequestConfigurer ignoring = builder.ignoring();
			// The ignores are not cumulative, so to prevent overwriting the defaults we
			// add them back.
			List<String> ignored = SpringBootWebSecurityConfiguration
					.getIgnored(this.security);
			if (!this.management.getSecurity().isEnabled()) {
				ignored.addAll(Arrays
						.asList(getEndpointPaths(this.endpointHandlerMapping)));
			}
			if (ignored.contains("none")) {
				ignored.remove("none");
			}
			if (this.errorController != null) {
				ignored.add(normalizePath(this.errorController.getErrorPath()));
			}
			if (this.server != null) {
				String[] paths = this.server.getPathsArray(ignored);
				ignoring.antMatchers(paths);
			}
		}

		private String normalizePath(String errorPath) {
			String result = StringUtils.cleanPath(errorPath);
			if (!result.startsWith("/")) {
				result = "/" + result;
			}
			return result;
		}

	}

	@Configuration
	@ConditionalOnMissingBean(WebSecurityConfiguration.class)
	@Conditional(WebSecurityEnablerCondition.class)
	@EnableWebSecurity
	protected static class WebSecurityEnabler extends AuthenticationManagerConfiguration {
	}

	/**
	 * WebSecurityEnabler condition
	 */
	static class WebSecurityEnablerCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			String managementEnabled = context.getEnvironment().getProperty(
					"management.security.enabled", "true");
			String basicEnabled = context.getEnvironment().getProperty(
					"security.basic.enabled", "true");
			return new ConditionOutcome("true".equalsIgnoreCase(managementEnabled)
					&& !"true".equalsIgnoreCase(basicEnabled),
					"Management security enabled and basic disabled");
		}

	}

	@Configuration
	@ConditionalOnMissingBean({ ManagementWebSecurityConfigurerAdapter.class })
	@ConditionalOnProperty(prefix = "management.security", name = "enabled", matchIfMissing = true)
	@Order(ManagementServerProperties.BASIC_AUTH_ORDER)
	protected static class ManagementWebSecurityConfigurerAdapter extends
			WebSecurityConfigurerAdapter {

		@Autowired
		private SecurityProperties security;

		@Autowired
		private ManagementServerProperties management;

		@Autowired(required = false)
		private ManagementContextResolver contextResolver;

		@Autowired(required = false)
		private ServerProperties server;

		@Autowired(required = false)
		private EndpointHandlerMapping endpointHandlerMapping;

		public void setEndpointHandlerMapping(
				EndpointHandlerMapping endpointHandlerMapping) {
			this.endpointHandlerMapping = endpointHandlerMapping;
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// secure endpoints
			RequestMatcher matcher = getRequestMatcher();
			if (matcher != null) {
				// Always protect them if present
				if (this.security.isRequireSsl()) {
					http.requiresChannel().anyRequest().requiresSecure();
				}
				AuthenticationEntryPoint entryPoint = entryPoint();
				http.exceptionHandling().authenticationEntryPoint(entryPoint);
				http.requestMatcher(matcher);
				configureAuthorizeRequests(new EndpointPathRequestMatcher(false),
						http.authorizeRequests());
				http.httpBasic().authenticationEntryPoint(entryPoint);
				// No cookies for management endpoints by default
				http.csrf().disable();
				http.sessionManagement().sessionCreationPolicy(
						this.management.getSecurity().getSessions());
				SpringBootWebSecurityConfiguration.configureHeaders(http.headers(),
						this.security.getHeaders());
			}
		}

		private RequestMatcher getRequestMatcher() {
			if (!this.management.getSecurity().isEnabled()) {
				return null;
			}
			String path = management.getContextPath();
			if (StringUtils.hasText(path)) {
				AntPathRequestMatcher matcher = new AntPathRequestMatcher(
						server.getPath(path) + "/**");
				return matcher;
			}
			return new EndpointPathRequestMatcher();
		}

		private AuthenticationEntryPoint entryPoint() {
			BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
			entryPoint.setRealmName(this.security.getBasic().getRealm());
			return entryPoint;
		}

		private void configureAuthorizeRequests(
				RequestMatcher permitAllMatcher,
				ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry requests) {
			requests.requestMatchers(permitAllMatcher).permitAll();
			requests.anyRequest().hasRole(this.management.getSecurity().getRole());
		}

		private final class EndpointPathRequestMatcher implements RequestMatcher {

			private boolean sensitive;

			private RequestMatcher delegate;

			public EndpointPathRequestMatcher(boolean sensitive) {
				this.sensitive = sensitive;
			}

			public EndpointPathRequestMatcher() {
				this(true);
			}

			@Override
			public boolean matches(HttpServletRequest request) {
				if (endpointHandlerMapping == null && contextResolver != null) {
					ApplicationContext context = contextResolver.getApplicationContext();
					if (context != null
							&& context.getBeanNamesForType(EndpointHandlerMapping.class).length > 0) {
						endpointHandlerMapping = context
								.getBean(EndpointHandlerMapping.class);
					}
				}
				if (endpointHandlerMapping == null) {
					endpointHandlerMapping = new EndpointHandlerMapping(
							Collections.<MvcEndpoint> emptySet());
				}
				if (delegate == null) {
					List<RequestMatcher> pathMatchers = new ArrayList<RequestMatcher>();
					String[] paths = !sensitive ? getEndpointPaths(
							endpointHandlerMapping, false)
							: getEndpointPaths(endpointHandlerMapping);
					for (String path : paths) {
						pathMatchers.add(new AntPathRequestMatcher(server.getPath(path)));
					}
					delegate = pathMatchers.isEmpty() ? AnyRequestMatcher.INSTANCE
							: new OrRequestMatcher(pathMatchers);
				}
				return delegate.matches(request);
			}
		}

	}

	private static String[] getEndpointPaths(EndpointHandlerMapping endpointHandlerMapping) {
		return StringUtils.mergeStringArrays(
				getEndpointPaths(endpointHandlerMapping, false),
				getEndpointPaths(endpointHandlerMapping, true));
	}

	private static String[] getEndpointPaths(
			EndpointHandlerMapping endpointHandlerMapping, boolean secure) {
		if (endpointHandlerMapping == null) {
			return NO_PATHS;
		}
		Set<? extends MvcEndpoint> endpoints = endpointHandlerMapping.getEndpoints();
		List<String> paths = new ArrayList<String>(endpoints.size());
		for (MvcEndpoint endpoint : endpoints) {
			if (endpoint.isSensitive() == secure) {
				String path = endpointHandlerMapping.getPath(endpoint.getPath());
				paths.add(path);
				// Ensure that nested paths are secured
				paths.add(path + "/**");
				// Add Spring MVC-generated additional paths
				paths.add(path + ".*");
			}
		}
		return paths.toArray(new String[paths.size()]);
	}

}
