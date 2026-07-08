package com.intellij.lang.puppet.psi.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import org.jetbrains.annotations.NotNull;

public class PuppetResolveResult extends PsiElementResolveResult {
  private final boolean myTextual;

  public PuppetResolveResult(@NotNull PsiElement element, boolean isTextual) {
    super(element);
    myTextual = isTextual;
  }

  public boolean isTextual() {
    return myTextual;
  }
}
