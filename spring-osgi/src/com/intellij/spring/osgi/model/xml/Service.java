// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml;

import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.converters.SpringBeanListConverter;
import com.intellij.spring.model.xml.BeanType;
import com.intellij.spring.model.xml.DomSpringBean;
import com.intellij.spring.model.xml.beans.SpringBean;
import com.intellij.spring.osgi.constants.SpringOsgiConstants;
import com.intellij.spring.osgi.model.converters.ServiceBeanRefConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@BeanType(SpringOsgiConstants.OSGI_SERVICE_FACTORY_BEAN_CLASSNAME)
public interface Service extends SpringOsgiDomElement, InterfacesOwner, DomSpringBean {

  @NotNull
  @Convert(ServiceBeanRefConverter.class)
  GenericAttributeValue<SpringBeanPointer<?>> getRef();

  @NotNull
  @Convert(value = SpringBeanListConverter.class)
  GenericAttributeValue<List<SpringBean>> getDependsOn();

  @NotNull
  GenericAttributeValue<ServiceClassLoaderOptions> getContextClassLoader();

  @NotNull
  GenericAttributeValue<AutoExportModes> getAutoExport();

  @NotNull
  GenericAttributeValue<Integer> getRanking();

  @NotNull
  ServiceProperties getServiceProperties();

  @NotNull
  List<ServiceRegistrationListener> getRegistrationListeners();

  ServiceRegistrationListener addRegistrationListener();

  SpringBean getBean();
}
