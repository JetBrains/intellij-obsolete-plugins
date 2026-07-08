package com.intellij.lang.puppet.psi.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public interface PuppetNamedPsiElementProcessor {
  void executeWithName(@NotNull String name, @NotNull PsiElement element);
}
