// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml;

import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.converters.SpringBeanResolveConverter;
import com.intellij.spring.model.xml.beans.SpringBean;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

public interface BasicListener extends SpringOsgiDomElement {

  @NotNull
  @Convert(SpringBeanResolveConverter.class)
  GenericAttributeValue<SpringBeanPointer<?>> getRef();

  SpringBean getBean();
}
