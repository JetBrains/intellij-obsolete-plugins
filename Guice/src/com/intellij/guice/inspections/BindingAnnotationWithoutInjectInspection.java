// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.extensions.GuiceBindingMatchStrategy;
import com.intellij.guice.utils.AnnotationUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UParameter;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

/**
 * Reports binding annotations (e.g., {@code @Named}, {@code @Qualifier}-annotated annotations)
 * used on fields or method parameters that are not themselves annotated with {@code @Inject}
 * (or {@code @Provides} / {@code @CheckedProvides} for method parameters). Without
 * {@code @Inject}, Guice ignores the binding annotation entirely.
 *
 * <p>Example:
 * <pre>
 * // Flagged: @Named is useless without @Inject on the field
 * {@literal @}Named("db") Connection connection;
 *
 * // OK: field is injected
 * {@literal @}Inject {@literal @}Named("db") Connection connection;
 *
 * // Flagged: parameter qualifier without @Inject on method
 * void configure({@literal @}Named("port") int port) {}
 *
 * // OK: method is annotated with @Provides
 * {@literal @}Provides Widget provide({@literal @}Named("port") int port) { ... }
 * </pre>
 */
public final class BindingAnnotationWithoutInjectInspection extends BaseUastInspection {
  public BindingAnnotationWithoutInjectInspection() {
    super(UAnnotation.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("binding.annotation.without.inject.problem.descriptor");
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
      if (!isBindingAnnotation(annotation)) {
        return true;
      }
      final UDeclaration owner = AnnotationUtils.resolveAnnotatedDeclaration(annotation);
      if (owner instanceof UField field) {
        if (!AnnotationUtils.isAnnotated(field, GuiceAnnotations.INJECTS)) {
          registerError(annotation);
        }
      }
      else if (owner instanceof UParameter parameter) {
        final UMethod uMethod = UastUtils.getParentOfType(parameter, UMethod.class);
        if (uMethod == null) {
          return true;
        }
        final PsiMethod containingMethod = uMethod.getJavaPsi();
        if (!isInjectOrProvides(containingMethod) && !isAssisted(annotation, containingMethod)) {
          registerError(annotation);
        }
      }
      return true;
    }

    /**
     * Tells if Guice calls the method: {@code @Inject}, or any {@code @Provides} annotation such as {@code @ProvidesIntoSet}.
     */
    private static boolean isInjectOrProvides(@NotNull PsiMethod method) {
      return AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, 0) ||
             AnnotationUtil.isAnnotated(method, GuiceBindingMatchStrategy.getAllProvidesAnnotations(), 0);
    }

    private static boolean isAssisted(@NotNull UAnnotation annotation, @NotNull PsiMethod method) {
      if (!GuiceAnnotations.ASSISTED.equals(annotation.getQualifiedName())) return false;
      if (method.isConstructor() && AnnotationUtil.isAnnotated(method, GuiceAnnotations.ASSISTED_INJECT, CHECK_HIERARCHY)) return true;
      PsiClass containingClass = method.getContainingClass();

      return containingClass != null && containingClass.isInterface();
    }
  }

  public static boolean isBindingAnnotation(@NotNull UAnnotation annotation) {
    final PsiClass annotationClass = annotation.resolve();
    if (annotationClass == null) {
      return false;
    }
    return AnnotationUtil.isAnnotated(annotationClass, GuiceAnnotations.BINDING_ANNOTATIONS, CHECK_HIERARCHY);
  }
}