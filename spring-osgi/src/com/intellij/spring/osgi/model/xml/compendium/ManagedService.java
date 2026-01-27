// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml.compendium;

import com.intellij.spring.model.xml.beans.Identified;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

// TODO unused
public interface ManagedService extends SpringOsgiCompendiumDomElement, Identified /*, SpringBean */{

  @NotNull
  GenericAttributeValue<Boolean> getPrimary();

  GenericAttributeValue<String> getPersistentId();

  @NotNull
  GenericAttributeValue<UpdateStrategy> getUpdateStrategy();

  @NotNull
  GenericAttributeValue<String> getUpdateMethod();

}
