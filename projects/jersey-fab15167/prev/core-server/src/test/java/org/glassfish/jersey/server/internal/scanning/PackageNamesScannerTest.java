/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.server.internal.scanning;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.fail;

/**
 * @author Eric Navarro
 */
public class PackageNamesScannerTest {

    private String jaxRsApiPath;
    private String[] packages = {"javax.ws.rs-api"};

    @Before
    public void setUp() throws Exception {
        final String classPath = System.getProperty("java.class.path");
        final String[] entries = classPath.split(System
                .getProperty("path.separator"));

        for (final String entry : entries) {
            if (entry.contains("javax.ws.rs-api")) {
                jaxRsApiPath = entry;
                break;
            }
        }

        if (jaxRsApiPath == null) {
            fail("Could not find javax.ws.rs-api.");
        }
    }

    @Test
    public void testWsJarScheme() {
        new PackageNamesScanner(createTestClassLoader("wsjar", createTestURLStreamHandler("wsjar"), jaxRsApiPath), packages,
                false);
    }

    @Test
    public void testJarScheme() {
        // Uses default class loader
        new PackageNamesScanner(packages, false);
    }

    @Test
    public void testZipScheme() {
        new PackageNamesScanner(createTestClassLoader("zip", createTestURLStreamHandler("zip"), jaxRsApiPath), packages, false);
    }

    @Test
    public void testFileScheme() {
        // Uses default class loader
        new PackageNamesScanner(packages, false);
    }

    @Test(expected = ResourceFinderException.class)
    public void testInvalidScheme() {
        new PackageNamesScanner(createTestClassLoader("bad", createTestURLStreamHandler("bad"), jaxRsApiPath), packages, false);
    }

    private ClassLoader createTestClassLoader(final String scheme,
                                              final URLStreamHandler urlStreamHandler,
                                              final String resourceFilePath) {
        return new ClassLoader() {
            public Enumeration<URL> getResources(String name) throws IOException {
                List<URL> list = new ArrayList<URL>();
                list.add((urlStreamHandler == null
                        ? new URL(null, scheme + ":" + resourceFilePath + "!/" + name)
                        : new URL(null, scheme + ":" + resourceFilePath + "!/" + name, urlStreamHandler)));
                return new Vector<URL>(list).elements();
            }
        };
    }

    // URLStreamHandler creation for the various schemes without having to add them as dependencies
    private URLStreamHandler createTestURLStreamHandler(final String scheme) {
        return new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void parseURL(URL u, String spec, int start, int limit) {
                setURL(u, scheme, "", -1, null, null, spec.substring(scheme.length() + 1), null, null);
            }
        };
    }
}
