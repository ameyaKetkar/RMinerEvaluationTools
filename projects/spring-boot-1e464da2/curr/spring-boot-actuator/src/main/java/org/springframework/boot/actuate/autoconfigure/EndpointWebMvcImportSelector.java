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
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Selects configuration classes for the Actuator MVC endpoints. Customize the MVC
 * endpoints by adding an entries to <code>/META-INF/spring.factories</code> under the
 * {@link EndpointWebMvcConfiguration} key.
 * 
 * @author Dave Syer
 *
 */
public class EndpointWebMvcImportSelector implements DeferredImportSelector,
		BeanClassLoaderAware {

	private ClassLoader beanClassLoader;

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		// Find all possible auto configuration classes, filtering duplicates
		List<String> factories = new ArrayList<String>(new LinkedHashSet<String>(
				SpringFactoriesLoader.loadFactoryNames(EndpointWebMvcConfiguration.class,
						this.beanClassLoader)));
		return factories.toArray(new String[0]);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		beanClassLoader = classLoader;
	}

}
