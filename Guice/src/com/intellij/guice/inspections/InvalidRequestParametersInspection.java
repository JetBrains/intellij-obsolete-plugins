// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

/**
 * Reports {@code @RequestParameters} annotations applied to variables whose type is not
 * {@code Map<String, String[]>}. Guice Servlet requires this exact type for injecting
 * HTTP request parameters.
 *
 * <p>Example:
 * <pre>
 * // Flagged: wrong type
 * {@literal @}Inject {@literal @}RequestParameters Map&lt;String, String&gt; params;
 *
 * // OK: correct type
 * {@literal @}Inject {@literal @}RequestParameters Map&lt;String, String[]&gt; params;
 * </pre>
 */
public final class InvalidRequestParametersInspection extends BaseUastInspection {
  public InvalidRequestParametersInspection() {
    super(UAnnotation.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("invalid.request.parameters.problem.descriptor");
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
    public boolean visitAnnotation(@NotNull UAnnotation annotation) {
      if (!"com.google.inject.servlet.RequestParameters".equals(annotation.getQualifiedName())) {
        return true;
      }
      final PsiVariable variable = PsiTreeUtil.getParentOfType(annotation.getSourcePsi(), PsiVariable.class);
      if (variable == null) {
        return true;
      }
      final PsiType type = variable.getType();
      String typeText = type.getCanonicalText();
      typeText = typeText.replaceAll(" ", "");
      if (typeText.equals("java.util.Map<java.lang.String,java.lang.String[]>")) {
        return true;
      }
      registerError(annotation);
      return true;
    }
  }
}