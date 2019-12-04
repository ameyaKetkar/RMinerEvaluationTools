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

package com.jfinal.i18n;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import com.jfinal.core.Const;
import com.jfinal.core.Controller;
import com.jfinal.kit.StrKit;
import com.jfinal.render.Render;

/**
 * I18nInterceptor is used to change the locale by request para,
 * and it is also switch the view or pass Res object to the view.
 * 
 * you can extends I18nInterceptor and override the getLocalePara() and getResName()
 * to customize configuration for your own i18n Interceptor
 */
public class I18nInterceptor implements Interceptor {
	
	private String localePara = "_locale";
	private String resName = "_res";
	private boolean isSwitchView = false;
	
	public I18nInterceptor() {
	}
	
	public I18nInterceptor(String localePara, String resName) {
		if (StrKit.isBlank(localePara))
			throw new IllegalArgumentException("localePara can not be blank.");
		if (StrKit.isBlank(resName))
			throw new IllegalArgumentException("resName can not be blank.");
		
		this.localePara = localePara;
		this.resName = resName;
	}
	
	public I18nInterceptor(String localePara, String resName, boolean isSwitchView) {
		this(localePara, resName);
		this.isSwitchView = isSwitchView;
	}
	
	/**
	 * Return the localePara, which is used as para name to get locale from the request para and the cookie.
	 */
	protected String getLocalePara() {
		return localePara;
	}
	
	/**
	 * Return the resName, which is used as attribute name to pass the Res object to the view.
	 */
	protected String getResName() {
		return resName;
	}
	
	/**
	 * Return the baseName, which is used as base name of the i18n resource file.
	 */
	protected String getBaseName() {
		return I18n.defaultBaseName;
	}
	
	/**
	 * 1: use the locale from request para if exists. change the locale write to the cookie
	 * 2: use the locale from cookie para if exists.
	 * 3: use the default locale
	 * 4: use setAttr(resName, resObject) pass Res object to the view.
	 */
	public void intercept(Invocation inv) {
		inv.invoke();
		
		Controller c = inv.getController();
		String localePara = getLocalePara();
		String locale = c.getPara(localePara);
		
		if (StrKit.notBlank(locale)) {	// change locale, write cookie
			c.setCookie(localePara, locale, Const.DEFAULT_I18N_MAX_AGE_OF_COOKIE);
		}
		else {							// get locale from cookie and use the default locale if it is null
			locale = c.getCookie(localePara);
			if (StrKit.isBlank(locale))
				locale = I18n.defaultLocale;
		}
		
		if (isSwitchView) {
			switchView(locale, c);
		}
		else {
			Res res = I18n.use(getBaseName(), locale);
			c.setAttr(getResName(), res);
		}
	}
	
	/**
	 * 在有些 web 系统中，页�?�需�?国际化的文本过多，并且 css 以�?� html 也因为际化而大�?相�?�，
	 * 对于这�?应用场景先直接制�?�多套�?��??称的国际化视图，并将这些视图以 locale 为�?目录分类存放，
	 * 最�?�使用本拦截器根�?� locale 动�?切�?�视图，而�?必对视图中的文本�?个进行国际化切�?�，�?时�?力。
	 */
	public void switchView(String locale, Controller c) {
		Render render = c.getRender();
		if (render != null) {
			String view = render.getView();
			if (view != null) {
				if (view.startsWith("/"))
					view = "/" + locale + view;
				else
					view = locale + "/" + view;
				
				render.setView(view);
			}
		}
	}
}




