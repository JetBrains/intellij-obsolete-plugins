// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.SetMultibindDescriptor;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Contributor for Guice {@code Multibinder} calls and {@code @ProvidesIntoSet} methods.
 */
final class SetMultibinderContributor implements GuiceBindingContributor {
  @Override
  public void register(@NotNull GuiceExtensionRegistrar registrar) {
    registrar.registerSingleTypeBinder(
        GuiceCallPattern.named("newSetBinder", "setBinder")
            .forBinder("com.google.inject.multibindings.Multibinder"),
        SetMultibindDescriptor::new
    );
    registrar.registerProvidesAnnotation(
        List.of(GuiceAnnotations.PROVIDES_INTO_SET, GuiceAnnotations.CHECKED_PROVIDES_INTO_SET),
        method -> {
          PsiType returnType = method.getReturnType();
          return returnType != null ? GuiceKeyForms.setForms(method, returnType) : List.of();
        }
    );
  }
}
