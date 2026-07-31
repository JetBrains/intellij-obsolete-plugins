package com.intellij.gwt.clientBundle.css.language.psi.impl;

import com.intellij.css.util.CssPsiUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.css.CssElement;
import com.intellij.psi.css.impl.CssElementTypes;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public abstract class GwtCssDeclarationElementBase extends CompositePsiElement implements CssElement, PsiNameIdentifierOwner {
  protected GwtCssDeclarationElementBase(IElementType type) {
    super(type);
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    final PsiElement token = getNameIdentifier();
    if (token != null) {
      CssPsiUtilCore.replaceToken(token, name);
    }
    return this;
  }

  @Override
  public String getName() {
    final PsiElement node = getNameIdentifier();
    return node != null ? node.getText() : "";
  }

  @Override
  public PsiElement getNameIdentifier() {
    return findPsiChildByType(CssElementTypes.CSS_IDENT);
  }

  @Override
  public int getTextOffset() {
    final PsiElement identifier = getNameIdentifier();
    return identifier != null ? identifier.getTextOffset() : super.getTextOffset();
  }
}
