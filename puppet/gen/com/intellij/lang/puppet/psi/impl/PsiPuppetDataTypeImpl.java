// This is a generated file. Not intended for manual editing.
package com.intellij.lang.puppet.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.lang.puppet.PuppetTokenTypes.*;
import com.intellij.lang.puppet.psi.mixins.PuppetDataTypeMixin;
import com.intellij.lang.puppet.psi.*;

public class PsiPuppetDataTypeImpl extends PuppetDataTypeMixin implements PsiPuppetDataType {

  public PsiPuppetDataTypeImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiPuppetVisitor visitor) {
    visitor.visitDataType(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiPuppetVisitor) accept((PsiPuppetVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public PsiPuppetAnyNameWrapper getAnyNameWrapper() {
    return notNullChild(PsiTreeUtil.getChildOfType(this, PsiPuppetAnyNameWrapper.class));
  }

  @Override
  @Nullable
  public PsiPuppetDataTypeParametersBlock getDataTypeParametersBlock() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetDataTypeParametersBlock.class);
  }

}
