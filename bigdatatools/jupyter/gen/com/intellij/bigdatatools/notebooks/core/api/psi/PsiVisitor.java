// This is a generated file. Not intended for manual editing.
package com.intellij.bigdatatools.notebooks.core.api.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.bigdatatools.notebooks.core.impl.psi.PsiCellBase;

public class PsiVisitor extends PsiElementVisitor {

  public void visitCell(@NotNull PsiCell o) {
    visitCellBase(o);
  }

  public void visitCellMagic(@NotNull PsiCellMagic o) {
    visitElement(o);
  }

  public void visitCellMarker(@NotNull PsiCellMarker o) {
    visitElement(o);
  }

  public void visitNotebook(@NotNull PsiNotebook o) {
    visitElement(o);
  }

  public void visitSource(@NotNull PsiSource o) {
    visitElement(o);
  }

  public void visitStemCell(@NotNull PsiStemCell o) {
    visitCellBase(o);
  }

  public void visitCellBase(@NotNull PsiCellBase o) {
    visitElement(o);
  }

  public void visitElement(@NotNull PsiElement o) {
    super.visitElement(o);
  }

}
