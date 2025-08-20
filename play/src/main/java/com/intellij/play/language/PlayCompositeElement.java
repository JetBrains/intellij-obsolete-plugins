/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class PlayCompositeElement extends ASTWrapperPsiElement{

  public PlayCompositeElement(@NotNull final ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public PsiFile getContainingFile() {
    return super.getContainingFile();
  }

  @Override
  public String toString() {
    return getNode().getElementType().toString();
  }
}
