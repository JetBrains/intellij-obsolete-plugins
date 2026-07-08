package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetQuotedString;
import com.intellij.lang.puppet.psi.impl.PuppetCompositePsiElementBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetQuotedStringMixin extends PuppetCompositePsiElementBase implements PuppetQuotedString {
  public PuppetQuotedStringMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable String getEffectiveTypeName() {
    return StringUtil.toLowerCase(ElementManipulators.getValueText(this));
  }

  @Override
  public boolean hasReferences() {
    return true;
  }
}
