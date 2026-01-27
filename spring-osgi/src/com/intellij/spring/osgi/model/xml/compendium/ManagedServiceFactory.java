// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml.compendium;

import com.intellij.spring.model.xml.beans.Identified;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

// TODO unused
public interface ManagedServiceFactory extends SpringOsgiCompendiumDomElement, Identified/*, SpringBean */{

  @NotNull
  GenericAttributeValue<Boolean> getPrimary();

  @NotNull
  @Required
  GenericAttributeValue<String> getFactoryPid();

  @NotNull
  GenericAttributeValue<UpdateStrategy> getUpdateStrategy();

  @NotNull
  GenericAttributeValue<String> getUpdateMethod();
}
