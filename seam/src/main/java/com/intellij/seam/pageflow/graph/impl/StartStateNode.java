package com.intellij.seam.pageflow.graph.impl;

import com.intellij.seam.pageflow.graph.PageflowNodeType;
import com.intellij.seam.pageflow.model.xml.pageflow.StartState;
import com.intellij.seam.pageflow.SeamPageflowIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class StartStateNode extends PageflowBasicNode<StartState> {

  public StartStateNode(@Nls String name, StartState identifyingElement) {
    super(identifyingElement, name);
  }

  @Override
  @NotNull
  public PageflowNodeType getNodeType() {
    return PageflowNodeType.START_STATE;
  }

  @Override
  public Icon getIcon() {
    return SeamPageflowIcons.Start;
  }
}

