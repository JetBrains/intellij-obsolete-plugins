package com.intellij.lang.puppet.psi;

import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class PuppetResourceInstanceDelegatingLightNamedElement extends PuppetDelegatingLightNamedElement {
  public PuppetResourceInstanceDelegatingLightNamedElement(@NotNull PuppetPolyNamedPsiElement delegate, String name) {
    super(delegate, name);
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    PuppetPsiUtil.renameResourceInstanceIdentifier(getNameIdentifier(), name);
    return this;
  }
}
