// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.tests;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.execution.JUnitRecognizer;
import com.intellij.execution.junit.JUnitUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.ext.spock.SpockUtils;

public final class GrailsJUnitRecognizer extends JUnitRecognizer {
  @Override
  public boolean isTestAnnotated(@NotNull PsiMethod method) {
    // See TestForTransformation.visit(...)
    String name = method.getName();
    if (!name.startsWith("test") ||
        name.indexOf('$') != -1 ||
        !method.getModifierList().hasModifierProperty(PsiModifier.PUBLIC) ||
        method.getModifierList().hasModifierProperty(PsiModifier.ABSTRACT) ||
        method.getParameterList().getParametersCount() != 0) {
      return false;
    }

    PsiClass containingClass = method.getContainingClass();
    if (containingClass == null) return false;

    if (!AnnotationUtil.isAnnotated(containingClass, GrailsTestUtils.TEST_ANNOTATIONS, 0)) {
      return false;
    }

    if (JUnitUtil.isJUnit3TestClass(containingClass)) return false;
    if (InheritanceUtil.isInheritor(containingClass, SpockUtils.SPEC_CLASS_NAME)) return false;

    return true;
  }
}
