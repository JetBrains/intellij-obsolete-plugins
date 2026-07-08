package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetAnyNameWrapper;
import com.intellij.lang.puppet.psi.impl.PuppetCompositePsiElementBase;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetAnyNameWrapperMixin extends PuppetCompositePsiElementBase implements PuppetAnyNameWrapper {
  public PuppetAnyNameWrapperMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable String getEffectiveTypeName() {
    return StringUtil.toLowerCase(getText());
  }

  @Override
  public boolean hasReferences() {
    return true;
  }
}
