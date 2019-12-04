/**
 * Copyright (c) 2011-2015, James Zhan 詹波 (jfinal@126.com).
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

package com.jfinal.ext.kit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import com.jfinal.plugin.activerecord.ModelRecordElResolver;

/**
 * 针对 weblogic 等部分容器无法 register ModelRecordElResolver 增强对象的情况，
 * 添加此 Listern 到 web.xml �?��?�解决
 * 
 * 用法，在 web.xml 中添加 ElResolverListener 的�?置如下：
 * <listener>
 * 		<listener-class>com.jfinal.ext.kit.ElResolverListener</listener-class>
 * </listener>
 */
public class ElResolverListener implements ServletContextListener {
	
	public void contextInitialized(ServletContextEvent sce) {
		ModelRecordElResolver.init(sce.getServletContext());
	}
	
	public void contextDestroyed(ServletContextEvent sce) {
		
	}
}



