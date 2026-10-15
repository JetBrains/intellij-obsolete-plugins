// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Extension point for contributing Guice binding and injection rules to the model.
 *
 * <p>Implementations register declarative call, method, field, and class patterns via
 * {@link #register(GuiceExtensionRegistrar)}. The plugin compiles all registered rules
 * into a {@link GuiceExtensionIndex} that filters candidates together in multiple stages.
 *
 * <h3>Thread safety</h3>
 * <p>Implementations must be stateless and thread-safe.
 */
@ApiStatus.Experimental
@ApiStatus.OverrideOnly
public interface GuiceBindingContributor {

  ExtensionPointName<GuiceBindingContributor> EP_NAME =
      ExtensionPointName.create("com.intellij.guice.bindingContributor");

  /**
   * Registers declarative call and annotation patterns for this contributor.
   */
  void register(@NotNull GuiceExtensionRegistrar registrar);
}
