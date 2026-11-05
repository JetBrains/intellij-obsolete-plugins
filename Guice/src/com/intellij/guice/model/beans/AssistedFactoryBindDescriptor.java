// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.SmartTypePointer;
import com.intellij.psi.SmartTypePointerManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

import java.util.List;

@ApiStatus.Experimental
public class AssistedFactoryBindDescriptor extends BindDescriptor {
  private final @Nullable SmartPsiElementPointer<PsiClass> myFactoryClass;
  private final @Nullable SmartTypePointer myFactoryPsiType;

  public AssistedFactoryBindDescriptor(@NotNull PsiElement callExpression, @Nullable PsiType factoryPsiType) {
    super(callExpression);
    PsiClass factoryClass = factoryPsiType instanceof PsiClassType ct ? ct.resolve() : null;
    myFactoryClass = factoryClass != null ? SmartPointerManager.createPointer(factoryClass) : null;
    myFactoryPsiType = factoryPsiType != null
                       ? SmartTypePointerManager.getInstance(callExpression.getProject()).createSmartTypePointer(factoryPsiType)
                       : null;
  }

  public @Nullable PsiClass getFactoryClass() {
    return myFactoryClass != null ? myFactoryClass.getElement() : null;
  }

  @Override
  public @Nullable PsiClass getBoundClass() {
    return getFactoryClass();
  }

  @Override
  public @Nullable PsiType getBoundType() {
    if (myFactoryPsiType != null) {
      return myFactoryPsiType.getType();
    }
    PsiClass cls = getFactoryClass();
    return cls != null ? JavaPsiFacade.getElementFactory(cls.getProject()).createType(cls) : null;
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    final UCallExpression uCall = getOutermostCall();
    if (uCall != null) {
      UCallExpression current = uCall;
      while (current != null) {
        final String name = current.getMethodName();
        if ("implement".equals(name)) {
          final List<UExpression> args = current.getValueArguments();
          if (args.size() > 1) {
            final PsiType implType = GuiceUtils.getBindingTypeFromExpression(args.getLast());
            if (implType instanceof PsiClassType) {
              return ((PsiClassType)implType).resolve();
            }
          }
        }
        current = GuiceUtils.getReceiverCall(current);
      }
    }
    PsiClass factory = getFactoryClass();
    return factory != null ? findFactoryProductType(factory) : null;
  }

  private static @Nullable PsiClass findFactoryProductType(@NotNull PsiClass factoryClass) {
    for (PsiMethod method : factoryClass.getMethods()) {
      if (method.hasModifierProperty(PsiModifier.ABSTRACT)) {
        final PsiType returnType = method.getReturnType();
        if (returnType instanceof PsiClassType) {
          return ((PsiClassType)returnType).resolve();
        }
      }
    }
    return null;
  }
}
