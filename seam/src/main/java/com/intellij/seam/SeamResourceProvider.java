/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.seam;

import com.intellij.javaee.ResourceRegistrar;
import com.intellij.javaee.StandardResourceProvider;
import com.intellij.seam.constants.SeamNamespaceConstants;

/**
 * @author Dmitry Avdeev
 */
public class SeamResourceProvider implements StandardResourceProvider{

  @Override
  public void registerResources(ResourceRegistrar registrar) {
    registrar.addStdResource("http://jboss.com/products/seam/components-2.0.xsd", "/resources/schemas/components-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/components-2.1.xsd", "/resources/schemas/components-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/components-2.2.xsd", "/resources/schemas/components-2.2.xsd", getClass());

    // core
    registrar.addStdResource("http://jboss.com/products/seam/core-2.0.xsd", "/resources/schemas/core-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/core-2.1.xsd", "/resources/schemas/core-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/core-2.2.xsd", "/resources/schemas/core-2.2.xsd", getClass());

    // persistence
    registrar.addStdResource("http://jboss.com/products/seam/persistence-2.0.xsd", "/resources/schemas/persistence-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/persistence-2.1.xsd", "/resources/schemas/persistence-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/persistence-2.2.xsd", "/resources/schemas/persistence-2.2.xsd", getClass());

    //security
    registrar.addStdResource("http://jboss.com/products/seam/security-2.0.xsd", "/resources/schemas/security-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/security-2.1.xsd", "/resources/schemas/security-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/security-2.2.xsd", "/resources/schemas/security-2.2.xsd", getClass());

    // theme
    registrar.addStdResource("http://jboss.com/products/seam/theme-2.0.xsd", "/resources/schemas/theme-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/theme-2.1.xsd", "/resources/schemas/theme-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/theme-2.2.xsd", "/resources/schemas/theme-2.2.xsd", getClass());

    // transaction
    registrar.addStdResource("http://jboss.com/products/seam/transaction-2.0.xsd", "/resources/schemas/transaction-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/transaction-2.1.xsd", "/resources/schemas/transaction-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/transaction-2.2.xsd", "/resources/schemas/transaction-2.2.xsd", getClass());

    // drools
    registrar.addStdResource("http://jboss.com/products/seam/drools-2.0.xsd", "/resources/schemas/drools-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/drools-2.1.xsd", "/resources/schemas/drools-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/drools-2.2.xsd", "/resources/schemas/drools-2.2.xsd", getClass());

    // framework
    registrar.addStdResource("http://jboss.com/products/seam/framework-2.0.xsd", "/resources/schemas/framework-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/framework-2.1.xsd", "/resources/schemas/framework-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/framework-2.2.xsd", "/resources/schemas/framework-2.2.xsd", getClass());

    // jms
    registrar.addStdResource("http://jboss.com/products/seam/jms-2.0.xsd", "/resources/schemas/jms-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/jms-2.1.xsd", "/resources/schemas/jms-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/jms-2.2.xsd", "/resources/schemas/jms-2.2.xsd", getClass());

    // mail
    registrar.addStdResource("http://jboss.com/products/seam/mail-2.0.xsd", "/resources/schemas/mail-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/mail-2.1.xsd", "/resources/schemas/mail-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/mail-2.2.xsd", "/resources/schemas/mail-2.2.xsd", getClass());

    // pdf
    registrar.addStdResource("http://jboss.com/products/seam/pdf-2.0.xsd", "/resources/schemas/pdf-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/pdf-2.2.xsd", "/resources/schemas/pdf-2.2.xsd", getClass());

    // remoting
    registrar.addStdResource("http://jboss.com/products/seam/remoting-2.0.xsd", "/resources/schemas/remoting-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/remoting-2.1.xsd", "/resources/schemas/remoting-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/remoting-2.2.xsd", "/resources/schemas/remoting-2.2.xsd", getClass());

    // spring integration
    registrar.addStdResource("http://jboss.com/products/seam/spring-2.0.xsd", "/resources/schemas/spring-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/spring-2.2.xsd", "/resources/schemas/spring-2.2.xsd", getClass());

    //web
    registrar.addStdResource("http://jboss.com/products/seam/web-2.0.xsd", "/resources/schemas/web-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/web-2.1.xsd", "/resources/schemas/web-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/web-2.2.xsd", "/resources/schemas/web-2.2.xsd", getClass());

    // pageflow
    registrar.addStdResource("http://jboss.com/products/seam/pageflow-2.0.xsd", "/resources/schemas/pageflow-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/pageflow-2.1.xsd", "/resources/schemas/pageflow-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/pageflow-2.2.xsd", "/resources/schemas/pageflow-2.2.xsd", getClass());

    // pages
    registrar.addStdResource("http://jboss.com/products/seam/pages-2.0.xsd", "/resources/schemas/pages-2.0.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/pages-2.1.xsd", "/resources/schemas/pages-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/pages-2.2.xsd", "/resources/schemas/pages-2.2.xsd", getClass());

    registrar.addStdResource(SeamNamespaceConstants.PAGES_DTD_2_0, "/resources/schemas/pages-2.0.dtd", getClass());
    registrar.addStdResource(SeamNamespaceConstants.PAGES_DTD_1_2, "/resources/schemas/pages-1.2.dtd", getClass());
    registrar.addStdResource(SeamNamespaceConstants.PAGES_DTD_1_1, "/resources/schemas/pages-1.1.dtd", getClass());

    // async
    registrar.addStdResource("http://jboss.com/products/seam/async-2.1.xsd",  "/resources/schemas/async-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/async-2.2.xsd",  "/resources/schemas/async-2.2.xsd", getClass());

    // bpm
    registrar.addStdResource("http://jboss.com/products/seam/bpm-2.1.xsd",  "/resources/schemas/bpm-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/bpm-2.2.xsd",  "/resources/schemas/bpm-2.2.xsd", getClass());

    // cache
    registrar.addStdResource("http://jboss.com/products/seam/cache-2.1.xsd",  "/resources/schemas/cache-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/cache-2.2.xsd",  "/resources/schemas/cache-2.2.xsd", getClass());

    // document
    registrar.addStdResource("http://jboss.com/products/seam/document-2.1.xsd",  "/resources/schemas/document-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/document-2.2.xsd",  "/resources/schemas/document-2.2.xsd", getClass());

    // international
    registrar.addStdResource("http://jboss.com/products/seam/interantional-2.1.xsd",  "/resources/schemas/international-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/interantional-2.2.xsd",  "/resources/schemas/international-2.2.xsd", getClass());

    // navigation
    registrar.addStdResource("http://jboss.com/products/seam/navigation-2.1.xsd",  "/resources/schemas/navigation-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/navigation-2.2.xsd",  "/resources/schemas/navigation-2.2.xsd", getClass());

    // ui
    registrar.addStdResource("http://jboss.com/products/seam/ui-2.1.xsd",  "/resources/schemas/ui-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/ui-2.2.xsd",  "/resources/schemas/ui-2.2.xsd", getClass());

    // wicket
    registrar.addStdResource("http://jboss.com/products/seam/wicket-2.1.xsd",  "/resources/schemas/wicket-2.1.xsd", getClass());
    registrar.addStdResource("http://jboss.com/products/seam/wicket-2.2.xsd",  "/resources/schemas/wicket-2.2.xsd", getClass());

    registrar.addStdResource("http://jboss.com/products/seam/resteasy-2.2.xsd",  "/resources/schemas/resteasy-2.2.xsd", getClass());
  }
}
