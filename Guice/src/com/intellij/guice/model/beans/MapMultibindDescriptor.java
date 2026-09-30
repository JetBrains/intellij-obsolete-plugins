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

public class MapMultibindDescriptor extends BindDescriptor {
  private final @Nullable SmartPsiElementPointer<PsiClass> myKeyType;
  private final @Nullable SmartPsiElementPointer<PsiClass> myValueType;
  private final @Nullable SmartTypePointer myKeyPsiType;
  private final @Nullable SmartTypePointer myValuePsiType;

  public MapMultibindDescriptor(@NotNull PsiElement callExpression, @Nullable PsiType keyPsiType, @Nullable PsiType valuePsiType) {
    super(callExpression);
    PsiClass keyClass = keyPsiType instanceof PsiClassType kct ? kct.resolve() : null;
    PsiClass valClass = valuePsiType instanceof PsiClassType vct ? vct.resolve() : null;
    myKeyType = keyClass != null ? SmartPointerManager.createPointer(keyClass) : null;
    myValueType = valClass != null ? SmartPointerManager.createPointer(valClass) : null;
    SmartTypePointerManager stpm = SmartTypePointerManager.getInstance(callExpression.getProject());
    myKeyPsiType = keyPsiType != null ? stpm.createSmartTypePointer(keyPsiType) : null;
    myValuePsiType = valuePsiType != null ? stpm.createSmartTypePointer(valuePsiType) : null;
  }

  public @Nullable PsiClass getKeyType() {
    return myKeyType != null ? myKeyType.getElement() : null;
  }

  public @Nullable PsiClass getValueType() {
    return myValueType != null ? myValueType.getElement() : null;
  }

  public @Nullable PsiType getKeyPsiType() {
    if (myKeyPsiType != null) {
      return myKeyPsiType.getType();
    }
    PsiClass cls = getKeyType();
    return cls != null ? JavaPsiFacade.getElementFactory(cls.getProject()).createType(cls) : null;
  }

  public @Nullable PsiType getValuePsiType() {
    if (myValuePsiType != null) {
      return myValuePsiType.getType();
    }
    PsiClass cls = getValueType();
    return cls != null ? JavaPsiFacade.getElementFactory(cls.getProject()).createType(cls) : null;
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    return null;
  }
}
