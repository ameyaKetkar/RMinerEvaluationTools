/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.security.messaging.access.expression;

import org.springframework.messaging.Message;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.core.Authentication;

/**
 * The {@link SecurityExpressionRoot} used for {@link Message} expressions.
 *
 * @since 4.0
 * @author Rob Winch
 */
public class MessageSecurityExpressionRoot extends SecurityExpressionRoot {

	public final Message<?> message;

	public MessageSecurityExpressionRoot(Authentication authentication, Message<?> message) {
		super(authentication);
		this.message = message;
	}
}
