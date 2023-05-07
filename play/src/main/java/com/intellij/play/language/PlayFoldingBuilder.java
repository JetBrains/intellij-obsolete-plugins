/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.play.language.psi.PlayPsiFile;
import com.intellij.play.language.psi.PlayTag;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlayFoldingBuilder implements FoldingBuilder, DumbAware {
  @Override
  public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull final ASTNode node, @NotNull final Document document) {
    final PsiElement element = node.getPsi();
    if (element instanceof PlayPsiFile file) {
      List<FoldingDescriptor> descriptors = new ArrayList<>();
      for (PlayTag playTag : file.getRootTags()) {
        addFoldingDescriptors(descriptors, playTag);
      }
      return descriptors.toArray(FoldingDescriptor.EMPTY_ARRAY);
    }
    return FoldingDescriptor.EMPTY_ARRAY;
  }

  private static void addFoldingDescriptors(final List<FoldingDescriptor> descriptors, final PlayTag tag) {
    if (!tag.textContains('\n')) return;

    PsiElement nameElement = tag.getNameElement();

    if (nameElement != null) {
      final int start = nameElement.getTextRange().getEndOffset();
      final int end = tag.getTextRange().getEndOffset() - 1;
      if (start + 1 < end) {
        descriptors.add(new FoldingDescriptor(tag.getNode(), new TextRange(start, end)));
        for (final PlayTag child : tag.getSubTags()) {
          addFoldingDescriptors(descriptors, child);
        }
      }
    }
  }

  @Override
  public String getPlaceholderText(@NotNull final ASTNode node) {
    return "...";
  }

  @Override
  public boolean isCollapsedByDefault(@NotNull final ASTNode node) {
    return false;
  }
}
