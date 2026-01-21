// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.utils;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.PsiImplUtil;
import org.jetbrains.annotations.Nullable;

public final class AnnotationUtils {
  public static @Nullable PsiElement findDefaultValue(PsiAnnotation annotation) {
    return PsiImplUtil.findDeclaredAttributeValue(annotation, PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
  }
}
