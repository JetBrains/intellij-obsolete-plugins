package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetResourceDeclaration;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.puppet.PuppetTokenTypes.AT;
import static com.intellij.lang.puppet.PuppetTokenTypes.ATAT;

public class PuppetResourceDeclarationMixin extends PuppetResourceDeclarationBaseMixin implements PuppetResourceDeclaration {

  public PuppetResourceDeclarationMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public boolean isVirtual() {
    return PsiUtilCore.getElementType(getFirstChild()) == AT;
  }

  @Override
  public boolean isExported() {
    return PsiUtilCore.getElementType(getFirstChild()) == ATAT;
  }
}
