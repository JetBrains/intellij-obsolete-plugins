// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.beans.MultimapBindDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Contributor for {@code MultimapBinder.newSetMultimapBinder()} and {@code multimapBinder()}.
 */
final class MultimapBinderContributor implements GuiceBindingContributor {
  @Override
  public void register(@NotNull GuiceExtensionRegistrar registrar) {
    registrar.registerDualTypeBinder(
        GuiceCallPattern.named("newSetMultimapBinder", "multimapBinder")
            .forBinder("com.google.common.inject.MultimapBinder"),
        MultimapBindDescriptor::new
    );
  }
}
