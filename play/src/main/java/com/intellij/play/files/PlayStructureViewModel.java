/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.files;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.play.language.PlayActionCompositeElement;
import com.intellij.play.language.PlayCompositeElement;
import com.intellij.play.language.psi.PlayNameValueCompositeElement;
import com.intellij.play.language.psi.PlayPsiFile;
import com.intellij.play.language.psi.PlayTag;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;

public class PlayStructureViewModel extends TextEditorBasedStructureViewModel {

  public PlayStructureViewModel(PsiFile file, Editor editor) {
    super(editor, file);
  }

  @Override
  @NotNull
  public StructureViewTreeElement getRoot() {
    return new PlayPsiFilePsiTreeElementBase((PlayPsiFile)getPsiFile());
  }

  private static class PlayTagPsiTreeElementBase extends PsiTreeElementBase<PlayTag> {
    PlayTagPsiTreeElementBase(@NotNull PlayTag playTag) {
      super(playTag);
    }

    @NotNull
    @Override
    public Collection<StructureViewTreeElement> getChildrenBase() {
      Collection<StructureViewTreeElement> children = new ArrayList<>();
      final PlayTag playTag = getElement();
      if (playTag != null && playTag.isValid()) {
        for (PlayTag subtag : playTag.getSubTags()) {
          children.add(new PlayTagPsiTreeElementBase(subtag));
        }
      }
      return children;
    }

    @Override
    public String getPresentableText() {
      final PlayTag playTag = getElement();
      if (playTag != null && playTag.isValid()) {
        return playTag.getName();
      }
      return "";
    }

    @Override
    public String getLocationString() {
      final PlayTag playTag = getElement();
      if (playTag != null && playTag.isValid()) {
        final PlayNameValueCompositeElement[] nameValues = playTag.getNameValues();

        if (nameValues.length != 0) {
          return elementsToString(nameValues);
        }
        final PlayActionCompositeElement[] actions = playTag.getActions();
        if (actions.length > 0) {
          return elementsToString(actions, "@");
        }
        return elementsToString(playTag.getTagExpressions());
      }

      return null;
    }

    @Nullable
    private static String elementsToString(PlayCompositeElement[] nameValues) {
      return elementsToString(nameValues, "");
    }

    @Nullable
    private static String elementsToString(PlayCompositeElement[] nameValues, String prefix) {
      if (nameValues.length == 0) return null;
      StringBuilder sb = new StringBuilder();
      for (PlayCompositeElement nameValue : nameValues) {
        sb.append(prefix);
        sb.append(nameValue.getText());
        sb.append(" ");
      }
      return sb.toString();
    }

    @NotNull
    @Override
    public ItemPresentation getPresentation() {
      return super.getPresentation();
    }

    @Override
    public Icon getIcon(boolean open) {
      return super.getIcon(open);
    }
  }

  private static class PlayPsiFilePsiTreeElementBase extends PsiTreeElementBase<PlayPsiFile> {
    PlayPsiFilePsiTreeElementBase(PlayPsiFile psiFile) {
      super(psiFile);
    }

    @Override
    @NotNull
    public Collection<StructureViewTreeElement> getChildrenBase() {
      Collection<StructureViewTreeElement> treeElements = new ArrayList<>();
      final PlayPsiFile file = getElement();

      if (file != null) {
        for (final PlayTag playTag : file.getRootTags()) {
          treeElements.add(new PlayTagPsiTreeElementBase(playTag));
        }
      }
      return treeElements;
    }

    @Override
    public String getPresentableText() {
      final PlayPsiFile file = getElement();
      return file == null ? "Play" : file.getPresentableName();
    }
  }
}
