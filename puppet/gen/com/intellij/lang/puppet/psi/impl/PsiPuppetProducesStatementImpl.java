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

public class PsiPuppetProducesStatementImpl extends PsiPuppetExpressionImpl implements PsiPuppetProducesStatement {

  public PsiPuppetProducesStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiPuppetVisitor visitor) {
    visitor.visitProducesStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiPuppetVisitor) accept((PsiPuppetVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<PsiPuppetAnyNameWrapper> getAnyNameWrapperList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, PsiPuppetAnyNameWrapper.class);
  }

  @Override
  @Nullable
  public PsiPuppetBracedAnyArgumentsListBlock getBracedAnyArgumentsListBlock() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetBracedAnyArgumentsListBlock.class);
  }

}
