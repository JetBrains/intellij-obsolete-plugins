// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.typeEnhancers.GrReferenceTypeEnhancer;
import org.jetbrains.plugins.groovy.lang.typing.PredefinedReturnType;

public final class PredefinedTypeReferenceEnhancer extends GrReferenceTypeEnhancer {
  @Override
  public @Nullable PsiType getReferenceType(GrReferenceExpression ref, @Nullable PsiElement resolved) {
    if (resolved != null) {
      return resolved.getUserData(PredefinedReturnType.PREDEFINED_RETURN_TYPE_KEY);
    }
    return null;
  }
}
