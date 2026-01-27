// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.model.xml.compendium;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

public interface ConfigProperties extends SpringOsgiCompendiumDomElement {

  /**
   * Returns the value of the persistent-id child.
   * <pre>
   * <h3>Attribute null:persistent-id documentation</h3>
   *         			The persistent id under which the properties to be exported are registered.
   * <p/>
   * </pre>
   *
   * @return the value of the persistent-id child.
   */
  @NotNull
  @Required
  GenericAttributeValue<String> getPersistentId();
}
