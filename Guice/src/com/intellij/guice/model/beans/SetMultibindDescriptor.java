// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.SmartTypePointer;
import com.intellij.psi.SmartTypePointerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SetMultibindDescriptor extends BindDescriptor {
  private final @Nullable SmartPsiElementPointer<PsiClass> myElementType;
  private final @Nullable SmartTypePointer myElementPsiType;

  public SetMultibindDescriptor(@NotNull PsiElement callExpression, @Nullable PsiType elementPsiType) {
    super(callExpression);
    PsiClass elementClass = elementPsiType instanceof PsiClassType ct ? ct.resolve() : null;
    myElementType = elementClass != null ? SmartPointerManager.createPointer(elementClass) : null;
    myElementPsiType = elementPsiType != null
                       ? SmartTypePointerManager.getInstance(callExpression.getProject()).createSmartTypePointer(elementPsiType)
                       : null;
  }

  public @Nullable PsiClass getElementType() {
    return myElementType != null ? myElementType.getElement() : null;
  }

  public @Nullable PsiType getElementPsiType() {
    if (myElementPsiType != null) {
      return myElementPsiType.getType();
    }
    PsiClass cls = getElementType();
    return cls != null ? JavaPsiFacade.getElementFactory(cls.getProject()).createType(cls) : null;
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    return null;
  }
}
