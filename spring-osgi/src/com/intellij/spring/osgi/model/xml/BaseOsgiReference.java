// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml;

import com.intellij.spring.model.converters.SpringBeanListConverter;
import com.intellij.spring.model.xml.DomSpringBean;
import com.intellij.spring.model.xml.beans.SpringBean;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface BaseOsgiReference extends DomSpringBean, InterfacesOwner, SpringOsgiDomElement {

  @NotNull
  GenericAttributeValue<String> getFilter();

  @Convert(value = SpringBeanListConverter.class)
  GenericAttributeValue<List<SpringBean>> getDependsOn();

  @NotNull
  @Attribute(value = "bean-name")
  GenericAttributeValue<String> getReferencedBeanName();

  @NotNull
  GenericAttributeValue<ReferenceClassLoaderOptions> getContextClassLoader();

  @NotNull
  List<Listener> getListeners();
}
