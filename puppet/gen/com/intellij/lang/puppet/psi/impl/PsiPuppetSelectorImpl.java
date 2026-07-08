// This is a generated file. Not intended for manual editing.
package com.intellij.lang.puppet.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.lang.puppet.PuppetTokenTypes.*;
import com.intellij.lang.puppet.psi.*;

public class PsiPuppetSelectorImpl extends PsiPuppetExpressionImpl implements PsiPuppetSelector {

  public PsiPuppetSelectorImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiPuppetVisitor visitor) {
    visitor.visitSelector(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiPuppetVisitor) accept((PsiPuppetVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiPuppetBracedSelectorValuesBlock getBracedSelectorValuesBlock() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetBracedSelectorValuesBlock.class);
  }

  @Override
  @NotNull
  public PsiPuppetExpression getExpression() {
    return notNullChild(PsiTreeUtil.getChildOfType(this, PsiPuppetExpression.class));
  }

  @Override
  @Nullable
  public PsiPuppetSelectorValue getSelectorValue() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetSelectorValue.class);
  }

}
