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

public class PsiPuppetCollectionImpl extends PsiPuppetExpressionImpl implements PsiPuppetCollection {

  public PsiPuppetCollectionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiPuppetVisitor visitor) {
    visitor.visitCollection(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiPuppetVisitor) accept((PsiPuppetVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiPuppetBracedAnyArgumentsListBlock getBracedAnyArgumentsListBlock() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetBracedAnyArgumentsListBlock.class);
  }

  @Override
  @NotNull
  public PsiPuppetCapitalizedNameWrapper getCapitalizedNameWrapper() {
    return notNullChild(PsiTreeUtil.getChildOfType(this, PsiPuppetCapitalizedNameWrapper.class));
  }

  @Override
  @NotNull
  public PsiPuppetCollectRHand getCollectRHand() {
    return notNullChild(PsiTreeUtil.getChildOfType(this, PsiPuppetCollectRHand.class));
  }

}
