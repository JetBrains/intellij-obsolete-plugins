// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

public interface NaturalOrdering extends SpringOsgiDomElement {

  @NotNull
  @Required
  GenericAttributeValue<OrderingBasis> getBasis();
}
