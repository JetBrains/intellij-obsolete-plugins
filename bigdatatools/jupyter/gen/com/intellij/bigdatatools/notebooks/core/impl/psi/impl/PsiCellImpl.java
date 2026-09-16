// This is a generated file. Not intended for manual editing.
package com.intellij.bigdatatools.notebooks.core.impl.psi.impl;

import com.intellij.bigdatatools.notebooks.core.api.psi.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiCellImpl extends ASTWrapperPsiElement implements PsiCell, NavigatablePsiElement {

  public PsiCellImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiVisitor visitor) {
    visitor.visitCell(this);
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
  @Nullable
  public PsiCellMagic getCellMagic() {
    return findChildByClass(PsiCellMagic.class);
  }

  @Override
  @NotNull
  public PsiCellMarker getCellMarker() {
    return findNotNullChildByClass(PsiCellMarker.class);
  }

  @Override
  @NotNull
  public PsiSource getSource() {
    return findNotNullChildByClass(PsiSource.class);
  }
}
