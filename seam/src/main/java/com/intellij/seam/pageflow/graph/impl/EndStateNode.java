package com.intellij.seam.pageflow.graph.impl;

import com.intellij.seam.pageflow.graph.PageflowNodeType;
import com.intellij.seam.pageflow.model.xml.pageflow.EndState;
import com.intellij.seam.pageflow.SeamPageflowIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class EndStateNode extends PageflowBasicNode<EndState> {

  public EndStateNode(@Nls String name, EndState identifyingElement) {
    super(identifyingElement, name);
  }

  @Override
  @NotNull
  public PageflowNodeType getNodeType() {
    return PageflowNodeType.END_STATE;
  }

  @Override
  public Icon getIcon() {
    return SeamPageflowIcons.End;
  }
}
