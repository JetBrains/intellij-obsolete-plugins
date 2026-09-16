// This is a generated file. Not intended for manual editing.
package com.intellij.bigdatatools.notebooks.core.api.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface PsiNotebook extends PsiElement {

  @NotNull
  List<PsiCell> getCellList();

  @Nullable
  PsiStemCell getStemCell();

}
