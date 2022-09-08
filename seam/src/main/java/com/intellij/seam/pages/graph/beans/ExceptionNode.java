package com.intellij.seam.pages.graph.beans;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.seam.pages.xml.pages.PagesException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ExceptionNode extends BasicPagesNode<PagesException>{
  public ExceptionNode(@NotNull PagesException identifyingElement, @Nullable @Nls String name) {
    super(identifyingElement, name);
  }

  @Override
  public @NlsSafe String getName() {
    return StringUtil.getShortName(getQualifiedName());
  }

  public String getQualifiedName() {
    return super.getName();
  }

  @Override
  public Icon getIcon() {
    return AllIcons.Nodes.ExceptionClass;
  }
}
