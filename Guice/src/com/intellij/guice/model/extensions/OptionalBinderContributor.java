// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.OptionalBindDescriptor;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Contributor for Guice {@code OptionalBinder} calls and {@code @ProvidesIntoOptional} methods.
 */
final class OptionalBinderContributor implements GuiceBindingContributor {
  @Override
  public void register(@NotNull GuiceExtensionRegistrar registrar) {
    registrar.registerSingleTypeBinder(
        GuiceCallPattern.named("newOptionalBinder", "optionalBinder")
            .forBinder("com.google.inject.multibindings.OptionalBinder"),
        OptionalBindDescriptor::new
    );
    registrar.registerProvidesAnnotation(
        List.of(GuiceAnnotations.PROVIDES_INTO_OPTIONAL),
        method -> {
          PsiType returnType = method.getReturnType();
          return returnType != null ? GuiceKeyForms.optionalForms(method, returnType, true) : List.of();
        }
    );
  }
}
