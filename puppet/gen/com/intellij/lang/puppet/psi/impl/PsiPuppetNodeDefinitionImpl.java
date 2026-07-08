// This is a generated file. Not intended for manual editing.
package com.intellij.lang.puppet.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.lang.puppet.PuppetTokenTypes.*;
import com.intellij.lang.puppet.psi.mixins.PuppetNodeDefinitionMixin;
import com.intellij.lang.puppet.psi.*;
import com.intellij.lang.puppet.psi.stubs.PuppetNodeDefinitionStub;
import com.intellij.psi.stubs.IStubElementType;

public class PsiPuppetNodeDefinitionImpl extends PuppetNodeDefinitionMixin implements PsiPuppetNodeDefinition {

  public PsiPuppetNodeDefinitionImpl(@NotNull PuppetNodeDefinitionStub stub, @NotNull IStubElementType type) {
    super(stub, type);
  }

  public PsiPuppetNodeDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiPuppetVisitor visitor) {
    visitor.visitNodeDefinition(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiPuppetVisitor) accept((PsiPuppetVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiPuppetBracedStatementsBlock getBracedStatementsBlock() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetBracedStatementsBlock.class);
  }

  @Override
  @Nullable
  public PsiPuppetNodeNamesList getNodeNamesList() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetNodeNamesList.class);
  }

  @Override
  @Nullable
  public PsiPuppetParentNode getParentNode() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetParentNode.class);
  }

}
