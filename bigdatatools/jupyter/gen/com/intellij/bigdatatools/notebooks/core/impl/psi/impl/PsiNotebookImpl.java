// This is a generated file. Not intended for manual editing.
package com.intellij.bigdatatools.notebooks.core.impl.psi.impl;

import com.intellij.bigdatatools.notebooks.core.api.psi.PsiCell;
import com.intellij.bigdatatools.notebooks.core.api.psi.PsiNotebook;
import com.intellij.bigdatatools.notebooks.core.api.psi.PsiStemCell;
import com.intellij.bigdatatools.notebooks.core.api.psi.PsiVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PsiNotebookImpl extends ASTWrapperPsiElement implements PsiNotebook {

  public PsiNotebookImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiVisitor visitor) {
    visitor.visitNotebook(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiVisitor) {
      accept((PsiVisitor)visitor);
    }
    else {
      super.accept(visitor);
    }
  }

  @Override
  @NotNull
  public List<PsiCell> getCellList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, PsiCell.class);
  }

  @Override
  @Nullable
  public PsiStemCell getStemCell() {
    return findChildByClass(PsiStemCell.class);
  }
}
