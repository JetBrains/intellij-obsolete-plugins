package com.intellij.seam.pageflow.graph.impl;

import com.intellij.seam.pageflow.graph.PageflowNodeType;
import com.intellij.seam.pageflow.model.xml.pageflow.PageElements;
import com.intellij.seam.pageflow.model.xml.pageflow.StartPage;
import com.intellij.seam.pageflow.SeamPageflowIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class PageNode extends PageflowBasicNode<PageElements> {

  public PageNode(@Nls String name, PageElements identifyingElement) {
    super(identifyingElement, name);
  }

  @Override
  @NotNull
  public PageflowNodeType getNodeType() {
    return getIdentifyingElement() instanceof StartPage ? PageflowNodeType.START_PAGE : PageflowNodeType.PAGE;
  }

  @Override
  public Icon getIcon() {
    return getIdentifyingElement() instanceof StartPage ? SeamPageflowIcons.StartPage : SeamPageflowIcons.Page;
  }
}
