// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.utils.MutationUtils;
import com.intellij.modcommand.ActionContext;
import com.intellij.modcommand.ModPsiUpdater;
import com.intellij.modcommand.Presentation;
import com.intellij.modcommand.PsiUpdateModCommandAction;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Toggles the {@code optional} attribute of a {@code com.google.inject.Inject} annotation.
 * Only the Guice annotation has this attribute, so the intention does not offer itself on
 * {@code javax.inject.Inject} or {@code jakarta.inject.Inject}.
 */
public final class ToggleInjectionRequiredIntention extends PsiUpdateModCommandAction<PsiAnnotation> {
  private static final String OPTIONAL_INJECT = "@com.google.inject.Inject(optional = true)";
  private static final String MANDATORY_INJECT = "@com.google.inject.Inject";

  public ToggleInjectionRequiredIntention() {
    super(PsiAnnotation.class);
  }

  @Override
  public @NotNull String getFamilyName() {
    return GuiceBundle.message("toggle.injection.required.family.name");
  }

  @Override
  protected boolean isElementApplicable(@NotNull PsiAnnotation element, @NotNull ActionContext context) {
    return new ToggleInjectionRequiredPredicate().satisfiedBy(element);
  }

  @Override
  protected @Nullable Presentation getPresentation(@NotNull ActionContext context, @NotNull PsiAnnotation element) {
    return Presentation.of(getTextForElement(element));
  }

  private static @NotNull String getTextForElement(@NotNull PsiAnnotation annotation) {
    PsiAnnotationMemberValue value = annotation.findAttributeValue("optional");
    if (value == null) {
      return GuiceBundle.message("make.injection.optional");
    }
    if (value instanceof PsiLiteralExpression) {
      return "false".equals(value.getText())
             ? GuiceBundle.message("make.injection.optional")
             : GuiceBundle.message("make.injection.mandatory");
    }
    return GuiceBundle.message("toggle.required");
  }

  @Override
  protected void invoke(@NotNull ActionContext context, @NotNull PsiAnnotation annotation, @NotNull ModPsiUpdater updater) {
    PsiAnnotationMemberValue value = annotation.findAttributeValue("optional");
    if (value == null) {
      MutationUtils.replaceAnnotation(annotation, OPTIONAL_INJECT);
    }
    else if (value instanceof PsiLiteralExpression) {
      MutationUtils.replaceAnnotation(annotation, "false".equals(value.getText()) ? OPTIONAL_INJECT : MANDATORY_INJECT);
    }
    else if (value instanceof PsiExpression expression) {
      MutationUtils.negateExpression(expression);
    }
  }
}
