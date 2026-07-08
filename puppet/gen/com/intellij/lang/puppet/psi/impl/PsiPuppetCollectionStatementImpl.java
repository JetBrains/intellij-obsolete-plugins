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

public class PsiPuppetCollectionStatementImpl extends PuppetCompositePsiElementBase implements PsiPuppetCollectionStatement {

  public PsiPuppetCollectionStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiPuppetVisitor visitor) {
    visitor.visitCollectionStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiPuppetVisitor) accept((PsiPuppetVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiPuppetCollExpr getCollExpr() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetCollExpr.class);
  }

  @Override
  @NotNull
  public List<PsiPuppetCollectionStatement> getCollectionStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, PsiPuppetCollectionStatement.class);
  }

}
