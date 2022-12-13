package com.intellij.dmserver.libraries.tree;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import icons.DmServerSupportIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public abstract class BundleItemElementBase<T> extends AbstractTreeNode<T> {

  public BundleItemElementBase(Project project, T key) {
    super(project, key);
  }

  protected static void updateIcons(PresentationData presentation) {
    presentation.setIcon(DmServerSupportIcons.Bundle);
  }

  protected static void updateStyledText(PresentationData presentation, @Nls String text, boolean waved) {
    presentation.addText(text,
                         new SimpleTextAttributes(waved ? SimpleTextAttributes.STYLE_WAVED : SimpleTextAttributes.STYLE_PLAIN,
                                                  presentation.getForcedTextForeground(), JBColor.RED));
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    return Collections.emptyList();
  }
}
