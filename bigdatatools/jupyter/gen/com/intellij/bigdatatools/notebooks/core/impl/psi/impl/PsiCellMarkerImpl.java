// This is a generated file. Not intended for manual editing.
package com.intellij.bigdatatools.notebooks.core.impl.psi.impl;

import com.intellij.bigdatatools.notebooks.core.api.psi.PsiCellMarker;
import com.intellij.bigdatatools.notebooks.core.api.psi.PsiVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class PsiCellMarkerImpl extends ASTWrapperPsiElement implements PsiCellMarker {

  public PsiCellMarkerImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiVisitor visitor) {
    visitor.visitCellMarker(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PsiVisitor) {
      accept((PsiVisitor)visitor);
    }
    else {
      super.accept(visitor);
    }
  }
}
