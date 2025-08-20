/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language.psi;

import com.intellij.lang.ASTNode;
import com.intellij.play.language.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.CheckUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class PlayTag extends PlayCompositeElement implements PsiNamedElement, ContributedReferenceHost {

  public PlayTag(@NotNull final ASTNode node) {
    super(node);
  }

  @Override
  public String getName() {
    final PsiElement nameElement = getNameElement();
    return nameElement == null ? "" : nameElement.getText();
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    CheckUtil.checkWritable(this);

    final PsiElement element = getNameElement();

    if (element != null) {
      final PlayPsiFile psiFile =
           (PlayPsiFile)PsiFileFactory.getInstance(getProject()).createFileFromText("play.html", PlayFileType.INSTANCE, "#{" + name + " }#{/" + name + " }");

      final PsiElement newElement = psiFile.getRootTags()[0].getNameElement();
      if (newElement != null ) element.replace(newElement);

      PsiElement endTagNameElement = getEndTagNameElement();
      if (endTagNameElement != null) {
        final PsiElement newEndTagElement = psiFile.getRootTags()[0].getEndTagNameElement();
        if (newEndTagElement != null ) {
          endTagNameElement.replace(newEndTagElement);
        }
      }
    }
    return this;
  }

  @Nullable
  public PsiElement getNameElement() {
    final PsiElement firstChild = getFirstChild();
    return firstChild == null ? null : firstChild.getNextSibling();
  }

  @Nullable
  public PsiElement getEndTagNameElement() {
    List<PsiElement> endTags = findChildrenByType(PlayElementTypes.END_TAG_START);
    if (endTags.size() == 1)  {
      return  endTags.get(0).getNextSibling();
    }
    return null;
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  public PlayTag @NotNull [] getSubTags() {
    return findChildrenByClass(PlayTag.class);
  }

  @Nullable
  public PlayNameValueCompositeElement findNameValue(@NotNull String name) {
    for (PlayNameValueCompositeElement nameValueCompositeElement : getNameValues()) {
      if (name.equals(nameValueCompositeElement.getName())) return nameValueCompositeElement;
    }
    return null;
  }

  public PlayNameValueCompositeElement @NotNull [] getNameValues() {
    return findChildrenByClass(PlayNameValueCompositeElement.class);
  }

  public TagExpressionCompositeElement @NotNull [] getTagExpressions() {
    return findChildrenByClass(TagExpressionCompositeElement.class);
  }

  public PlayActionCompositeElement @NotNull [] getActions() {
    return findChildrenByClass(PlayActionCompositeElement.class);
  }

  @Override
  public Icon getElementIcon(final int flags) {
    return PlatformIcons.XML_TAG_ICON;
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    return PsiReferenceService.getService().getContributedReferences(this);
  }
}
