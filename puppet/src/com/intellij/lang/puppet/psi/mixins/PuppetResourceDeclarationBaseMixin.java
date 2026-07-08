package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetResourceDeclarationBase;
import com.intellij.lang.puppet.psi.PuppetTypeHolder;
import com.intellij.lang.puppet.psi.impl.PuppetCompositePsiElementBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetResourceDeclarationBaseMixin extends PuppetCompositePsiElementBase implements PuppetResourceDeclarationBase {
  public PuppetResourceDeclarationBaseMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable String getEffectiveTypeName() {
    PuppetTypeHolder typeHolder = findChildByClass(PuppetTypeHolder.class);
    return typeHolder == null ? null : typeHolder.getEffectiveTypeName();
  }
}
