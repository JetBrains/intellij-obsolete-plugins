// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.inspections;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.extensions.GuiceCallPattern;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.UClass;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.openapi.util.Ref;
import com.intellij.guice.constants.GuiceClasses;

/**
 * Reports pointless untargeted {@code bind()} calls where the bound class has no
 * {@code @Inject}-annotated constructors, fields, or methods, making the binding useless.
 */
public final class PointlessBindingInspection extends BaseUastInspection {
  public PointlessBindingInspection() {
    GuiceCallPattern bindStatement = GuiceCallPattern.named("bind")
        .requireValueOrTypeArgument()
        .statementOnly();
    extendCall(
        // bind() of Guice itself, or an extension function (Kotlin) on a Module or Binder receiver
        GuiceCallPattern.anyOf(
            bindStatement.inGuicePackage(),
            bindStatement.withReceiverInheritor("com.google.inject.Module", "com.google.inject.Binder")),
        PointlessBindingInspection::checkBindCall
    );
  }

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return GuiceBundle.message("pointless.binding.problem.descriptor");
  }

  @Override
  public LocalQuickFix buildFix() {
    return new DeleteBindingFix(DeleteBindingFix.Mode.STATEMENT, "delete.binding");
  }

  private static void checkBindCall(@NotNull UCallExpression expression, @NotNull BaseUastInspectionVisitor visitor) {
    PsiClass psiClass = GuiceUtils.resolveClassArgument(expression);
    if (psiClass == null || usesInject(psiClass)) {
      return;
    }
    if (requiresExplicitBindings(expression)) {
      return;
    }
    visitor.registerError(expression);
  }

  /**
   * Tells if the module needs an explicit binding for every class it uses: a {@code PrivateModule} (an exposed
   * class needs a binding inside the private module) or a module that calls {@code requireExplicitBindings()}.
   */
  private static boolean requiresExplicitBindings(@NotNull UCallExpression bindCall) {
    UClass moduleClass = UastUtils.getParentOfType(bindCall, UClass.class);
    if (moduleClass == null) return false;
    if (InheritanceUtil.isInheritor(moduleClass.getJavaPsi(), GuiceClasses.PRIVATE_MODULE)) return true;
    Ref<Boolean> found = new Ref<>(false);
    moduleClass.accept(new AbstractUastVisitor() {
      @Override
      public boolean visitCallExpression(@NotNull UCallExpression node) {
        if ("requireExplicitBindings".equals(node.getMethodName())) {
          found.set(true);
        }
        return found.get() || super.visitCallExpression(node);
      }
    });
    return found.get();
  }

  private static boolean usesInject(PsiClass aClass) {
    for (PsiMethod method : aClass.getAllMethods()) {
      if (AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, 0)) {
        return true;
      }
    }
    for (PsiField field : aClass.getAllFields()) {
      if (AnnotationUtil.isAnnotated(field, GuiceAnnotations.INJECTS, 0)) {
        return true;
      }
    }
    return false;
  }
}
