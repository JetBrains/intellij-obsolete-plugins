// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DelegateReference extends PsiReferenceBase<PsiElement> {

  private volatile boolean myDelegateInit;

  private volatile PsiReference myDelegate;

  private Boolean mySoft;

  public DelegateReference(PsiElement element, TextRange range) {
    super(element, range);
  }

  public DelegateReference(@NotNull PsiElement element) {
    super(element);
  }

  public DelegateReference(@NotNull PsiElement element, boolean isSoft) {
    super(element);
    mySoft = isSoft;
  }

  protected abstract @Nullable PsiReference createDelegate();

  private void ensureInit() {
    if (myDelegateInit) return;

    myDelegate = createDelegate();
    myDelegateInit = true;
  }

  @Override
  public PsiElement resolve() {
    ensureInit();
    return myDelegate == null ? null : myDelegate.resolve();
  }

  @Override
  public Object @NotNull [] getVariants() {
    ensureInit();
    return myDelegate == null ? ArrayUtilRt.EMPTY_OBJECT_ARRAY : myDelegate.getVariants();
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    ensureInit();
    return myDelegate == null ? myElement : myDelegate.handleElementRename(newElementName);
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    ensureInit();
    return myDelegate == null ? myElement : myDelegate.bindToElement(element);
  }

  @Override
  public boolean isReferenceTo(@NotNull PsiElement element) {
    ensureInit();
    return myDelegate != null && myDelegate.isReferenceTo(element);
  }

  @Override
  public boolean isSoft() {
    if (mySoft != null) {
      return mySoft;
    }
    ensureInit();
    return myDelegate == null || myDelegate.isSoft();
  }
}
