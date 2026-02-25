// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.util;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class SafeReference implements PsiReference {

  private final PsiReferenceBase myDelegate;

  private final SafeReference myNextReference;

  public SafeReference(PsiReferenceBase delegate, SafeReference nextReference) {
    myDelegate = delegate;
    myNextReference = nextReference;
  }

  @Override
  public @NotNull PsiElement getElement() {
    return myDelegate.getElement();
  }

  @Override
  public @NotNull TextRange getRangeInElement() {
    return myDelegate.getRangeInElement();
  }

  @Override
  public PsiElement resolve() {
    return myDelegate.resolve();
  }

  @Override
  public @NotNull String getCanonicalText() {
    return myDelegate.getCanonicalText();
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    int lengthBefore = myDelegate.getRangeInElement().getLength();

    PsiElement res = myDelegate.handleElementRename(newElementName);

    int delta = myDelegate.getRangeInElement().getLength() - lengthBefore;

    for (SafeReference ref = myNextReference; ref != null; ref = ref.myNextReference) {
      ref.myDelegate.setRangeInElement(ref.getRangeInElement().shiftRight(delta));
    }

    return res;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    return myDelegate.bindToElement(element);
  }

  @Override
  public boolean isReferenceTo(@NotNull PsiElement element) {
    return myDelegate.isReferenceTo(element);
  }

  @Override
  public Object @NotNull [] getVariants() {
    return myDelegate.getVariants();
  }

  @Override
  public boolean isSoft() {
    return myDelegate.isSoft();
  }

  public static void makeReferencesSafe(PsiReference @NotNull [] referencesInSameElement) {
    if (referencesInSameElement.length < 2) return;

    int last = referencesInSameElement.length - 1;

    referencesInSameElement[last] = new SafeReference((PsiReferenceBase)referencesInSameElement[last], null);

    for (int i = last; i > 0; i--) {
      PsiReference ref1 = referencesInSameElement[i - 1];
      PsiReference ref2 = referencesInSameElement[i];

      assert ref1.getRangeInElement().getEndOffset() <= ref2.getRangeInElement().getStartOffset();
      assert ref1.getElement() == ref2.getElement();

      referencesInSameElement[i - 1] = new SafeReference((PsiReferenceBase)ref1, (SafeReference)ref2);
    }
  }

}
