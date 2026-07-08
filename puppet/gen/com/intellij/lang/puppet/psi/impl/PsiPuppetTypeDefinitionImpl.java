// This is a generated file. Not intended for manual editing.
package com.intellij.lang.puppet.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.lang.puppet.PuppetTokenTypes.*;
import com.intellij.lang.puppet.psi.mixins.PuppetTypeDefinitionMixin;
import com.intellij.lang.puppet.psi.*;
import com.intellij.lang.puppet.psi.stubs.PuppetTypeDefinitionStub;
import com.intellij.psi.stubs.IStubElementType;

public class PsiPuppetTypeDefinitionImpl extends PuppetTypeDefinitionMixin implements PsiPuppetTypeDefinition {

  public PsiPuppetTypeDefinitionImpl(@NotNull PuppetTypeDefinitionStub stub, @NotNull IStubElementType type) {
    super(stub, type);
  }

  public PsiPuppetTypeDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiPuppetVisitor visitor) {
    visitor.visitTypeDefinition(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiPuppetVisitor) accept((PsiPuppetVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<PsiPuppetBlock> getBlockList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, PsiPuppetBlock.class);
  }

  @Override
  @Nullable
  public PsiPuppetFqnContainer getFqnContainer() {
    return PsiTreeUtil.getChildOfType(this, PsiPuppetFqnContainer.class);
  }

}
