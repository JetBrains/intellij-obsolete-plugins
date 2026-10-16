// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.utils.AnnotationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor;

/**
 * Reports conflicting Guice annotations on the same class or element. The following
 * combinations are flagged as mutually exclusive:
 * <ul>
 *   <li>{@code @ImplementedBy} and {@code @ProvidedBy}</li>
 *   <li>{@code @Singleton} with {@code @SessionScoped} or {@code @RequestScoped}</li>
 *   <li>{@code @SessionScoped} with {@code @RequestScoped}</li>
 * </ul>
 *
 * <p>Example:
 * <pre>
 * // Flagged: conflicting default-binding annotations
 * {@literal @}ImplementedBy(FooImpl.class)
 * {@literal @}ProvidedBy(FooProvider.class)
 * interface Foo {}
 *
 * // Flagged: conflicting scope annotations
 * {@literal @}Singleton
 * {@literal @}RequestScoped
 * class Bar {}
 * </pre>
 */
public final class ConflictingAnnotationsInspection extends BaseUastInspection {
  public ConflictingAnnotationsInspection() {
    super(UAnnotation.class);
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("conflicting.annotations.problem.descriptor");
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
      final String qualifiedName = annotation.getQualifiedName();
      if (qualifiedName == null) {
        return true;
      }
      final UDeclaration owner = AnnotationUtils.resolveAnnotatedDeclaration(annotation);
      if (owner == null) {
        return true;
      }
      if (GuiceAnnotations.IMPLEMENTED_BY.equals(qualifiedName)) {
        if (AnnotationUtils.isAnnotated(owner, GuiceAnnotations.PROVIDED_BY)) {
          registerError(annotation);
        }
        return true;
      }
      if (GuiceAnnotations.PROVIDED_BY.equals(qualifiedName)) {
        if (AnnotationUtils.isAnnotated(owner, GuiceAnnotations.IMPLEMENTED_BY)) {
          registerError(annotation);
        }
        return true;
      }
      if (GuiceAnnotations.SINGLETONS.contains(qualifiedName)) {
        if (AnnotationUtils.isAnnotated(owner, GuiceAnnotations.SESSION_SCOPED) ||
            AnnotationUtils.isAnnotated(owner, GuiceAnnotations.REQUEST_SCOPED)) {
          registerError(annotation);
        }
        return true;
      }
      if (GuiceAnnotations.SESSION_SCOPED.equals(qualifiedName)) {
        if (AnnotationUtils.isAnnotated(owner, GuiceAnnotations.SINGLETONS) ||
            AnnotationUtils.isAnnotated(owner, GuiceAnnotations.REQUEST_SCOPED)) {
          registerError(annotation);
        }
        return true;
      }
      if (GuiceAnnotations.REQUEST_SCOPED.equals(qualifiedName)) {
        if (AnnotationUtils.isAnnotated(owner, GuiceAnnotations.SESSION_SCOPED) ||
            AnnotationUtils.isAnnotated(owner, GuiceAnnotations.SINGLETONS)) {
          registerError(annotation);
        }
      }
      return true;
    }
  }
}