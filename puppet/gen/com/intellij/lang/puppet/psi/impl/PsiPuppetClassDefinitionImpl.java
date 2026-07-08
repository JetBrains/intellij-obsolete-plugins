// This is a generated file. Not intended for manual editing.
package com.intellij.lang.puppet.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.lang.puppet.PuppetTokenTypes.*;
import com.intellij.lang.puppet.psi.mixins.PuppetClassDefinitionMixin;
import com.intellij.lang.puppet.psi.*;
import com.intellij.lang.puppet.psi.stubs.PuppetClassDefinitionStub;
import com.intellij.psi.stubs.IStubElementType;

public class PsiPuppetClassDefinitionImpl extends PuppetClassDefinitionMixin implements PsiPuppetClassDefinition {

  public PsiPuppetClassDefinitionImpl(@NotNull PuppetClassDefinitionStub stub, @NotNull IStubElementType type) {
    super(stub, type);
  }

  public PsiPuppetClassDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiPuppetVisitor visitor) {
    visitor.visitClassDefinition(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiPuppetVisitor) accept((PsiPuppetVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiPuppetAnyNameWrapper getAnyNameWrapper() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetAnyNameWrapper.class);
  }

  @Override
  @Nullable
  public PsiPuppetBracedStatementsBlock getBracedStatementsBlock() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetBracedStatementsBlock.class);
  }

  @Override
  @Nullable
  public PsiPuppetFqnContainer getFqnContainer() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetFqnContainer.class);
  }

  @Override
  @Nullable
  public PsiPuppetParenthesizedParametersListBlock getParenthesizedParametersListBlock() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetParenthesizedParametersListBlock.class);
  }

}
