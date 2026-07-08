package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetAnonymousBlock;
import com.intellij.lang.puppet.psi.impl.PuppetCompositePsiElementBase;
import org.jetbrains.annotations.NotNull;

public abstract class PuppetAnonymousBlockMixin extends PuppetCompositePsiElementBase implements PuppetAnonymousBlock {
  public PuppetAnonymousBlockMixin(@NotNull ASTNode node) {
    super(node);
  }
}
