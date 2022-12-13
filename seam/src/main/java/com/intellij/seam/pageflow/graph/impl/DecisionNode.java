package com.intellij.seam.pageflow.graph.impl;

import com.intellij.seam.pageflow.graph.PageflowNodeType;
import com.intellij.seam.pageflow.model.xml.pageflow.Decision;
import com.intellij.seam.pageflow.SeamPageflowIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DecisionNode extends PageflowBasicNode<Decision> {

  public DecisionNode(@Nls String name, Decision identifyingElement) {
    super(identifyingElement, name);
  }

  @Override
  @NotNull
  public PageflowNodeType getNodeType() {
    return PageflowNodeType.DECISIION;
  }

  @Override
  public Icon getIcon() {
    return SeamPageflowIcons.Decision;
  }
}
