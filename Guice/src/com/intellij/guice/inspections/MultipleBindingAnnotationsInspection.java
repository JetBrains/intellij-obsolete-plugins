// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.utils.AnnotationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UParameter;
import org.jetbrains.uast.UVariable;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

/**
 * Reports fields or parameters annotated with more than one binding annotation
 * (i.e., annotations themselves annotated with {@code @BindingAnnotation} or
 * {@code @Qualifier}). Guice allows at most one binding annotation per injection point.
 *
 * <p>Example:
 * <pre>
 * // Flagged: two binding annotations on the same field
 * {@literal @}Inject {@literal @}Named("a") {@literal @}Blue Widget widget;
 *
 * // OK: single binding annotation
 * {@literal @}Inject {@literal @}Named("a") Widget widget;
 * </pre>
 */
public final class MultipleBindingAnnotationsInspection extends BaseUastInspection {
  public MultipleBindingAnnotationsInspection() {
    super(UField.class, UParameter.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("multiple.binding.annotations.problem.descriptor");
  }

  @Override
  public @NotNull AbstractUastNonRecursiveVisitor buildUastVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new Visitor(this, holder, isOnTheFly);
  }

  private static class Visitor extends BaseUastInspectionVisitor {
    Visitor(@NotNull BaseUastInspection inspection, @NotNull ProblemsHolder holder, boolean onTheFly) {
      super(inspection, holder, onTheFly);
    }

    @Override
    public boolean visitField(@NotNull UField node) {
      checkVariable(node);
      return true;
    }

    @Override
    public boolean visitParameter(@NotNull UParameter node) {
      checkVariable(node);
      return true;
    }

    private void checkVariable(@NotNull UVariable variable) {
      int numBindingAnnotations = 0;
      for (UAnnotation annotation : AnnotationUtils.collectDeclarationUAnnotations(variable)) {
        if (BindingAnnotationWithoutInjectInspection.isBindingAnnotation(annotation)) {
          numBindingAnnotations++;
        }
      }
      if (numBindingAnnotations > 1) {
        final UElement nameIdentifier = variable.getUastAnchor();
        registerError(nameIdentifier != null ? nameIdentifier : variable);
      }
    }
  }
}