// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml;

import com.intellij.psi.CommonClassNames;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.converters.SpringBeanResolveConverter;
import com.intellij.spring.model.xml.RequiredBeanType;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

public interface BaseReferenceCollection extends SpringOsgiDomElement, BaseOsgiReference {

  @NotNull
  @Convert(SpringBeanResolveConverter.class)
  @RequiredBeanType(CommonClassNames.JAVA_UTIL_COMPARATOR)
  GenericAttributeValue<SpringBeanPointer<?>> getComparatorRef();

  @NotNull
  GenericAttributeValue<CollectionCardinality> getCardinality();

  @NotNull
  Comparator getComparator();
}
