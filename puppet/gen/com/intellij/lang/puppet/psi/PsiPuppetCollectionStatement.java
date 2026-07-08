// This is a generated file. Not intended for manual editing.
package com.intellij.lang.puppet.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface PsiPuppetCollectionStatement extends PuppetCompositePsiElement {

  @Nullable
  PsiPuppetCollExpr getCollExpr();

  @NotNull
  List<PsiPuppetCollectionStatement> getCollectionStatementList();

}
