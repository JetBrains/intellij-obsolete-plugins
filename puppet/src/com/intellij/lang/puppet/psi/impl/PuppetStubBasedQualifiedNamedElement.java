package com.intellij.lang.puppet.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedPsiElementBase;
import com.intellij.lang.puppet.psi.stubs.PuppetStubElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;

public abstract class PuppetStubBasedQualifiedNamedElement<S extends PuppetStubElement> extends PuppetStubBasedPsiElementBase<S>
  implements PsiNameIdentifierOwner {

  public PuppetStubBasedQualifiedNamedElement(@NotNull S stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetStubBasedQualifiedNamedElement(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public int getTextOffset() {
    PsiElement identifier = getNameIdentifier();
    return identifier == null ? super.getTextOffset() : identifier.getTextOffset();
  }

  @Override
  public @NotNull PsiElement getNavigationElement() {
    PsiElement identifier = getNameIdentifier();
    return identifier == null ? super.getNavigationElement() : identifier;
  }
}
