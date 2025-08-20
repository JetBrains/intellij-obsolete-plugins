/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language.psi;

import com.intellij.lang.ASTNode;
import com.intellij.play.language.PlayActionCompositeElement;
import com.intellij.play.language.PlayCompositeElement;
import com.intellij.play.language.TagExpressionCompositeElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayNameValueCompositeElement extends PlayCompositeElement implements PsiNamedElement {

  public PlayNameValueCompositeElement(@NotNull final ASTNode node) {
    super(node);
  }

  @Override
  public String getName() {
      final PsiElement nameElement = getNameElement();
      return nameElement == null ? "" : nameElement.getText();
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    return null;
  }

  @Nullable
  public String getValue() {
      final PsiElement valueElement = getValueElement();
      return valueElement == null ? null : valueElement.getText();
  }

  @Nullable
  public PsiElement getNameElement() {
      return getFirstChild();
  }

  @Nullable
  public PsiElement getValueElement() {
    final TagExpressionCompositeElement tagExpression = findChildByClass(TagExpressionCompositeElement.class);

    return tagExpression == null ? findChildByClass(PlayActionCompositeElement.class) : tagExpression;
  }

}
