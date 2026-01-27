// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml;

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.ExtendClass;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

public interface InterfacesOwner extends SpringOsgiDomElement {

  @NotNull
  @ExtendClass(instantiatable = false, allowEnum = false)
  GenericAttributeValue<PsiClass> getInterface();

  @NotNull
  Interfaces getInterfaces();
}