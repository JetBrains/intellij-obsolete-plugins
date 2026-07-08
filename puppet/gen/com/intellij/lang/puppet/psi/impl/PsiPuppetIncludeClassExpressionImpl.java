// This is a generated file. Not intended for manual editing.
package com.intellij.lang.puppet.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.lang.puppet.PuppetTokenTypes.*;
import com.intellij.lang.puppet.psi.mixins.PuppetIncludeClassExpressionMixin;
import com.intellij.lang.puppet.psi.*;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetIncludeClassStatementStub;
import com.intellij.psi.stubs.IStubElementType;

public class PsiPuppetIncludeClassExpressionImpl extends PuppetIncludeClassExpressionMixin implements PsiPuppetIncludeClassExpression {

  public PsiPuppetIncludeClassExpressionImpl(@NotNull PuppetIncludeClassStatementStub stub, @NotNull IStubElementType type) {
    super(stub, type);
  }

  public PsiPuppetIncludeClassExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiPuppetVisitor visitor) {
    visitor.visitIncludeClassExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiPuppetVisitor) accept((PsiPuppetVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiPuppetResourceLikeClassDeclarationBlock getResourceLikeClassDeclarationBlock() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetResourceLikeClassDeclarationBlock.class);
  }

}
