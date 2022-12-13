package com.intellij.seam.pageflow.graph.impl;

import com.intellij.seam.pageflow.graph.PageflowNodeType;
import com.intellij.seam.pageflow.model.xml.pageflow.ProcessState;
import com.intellij.seam.pageflow.SeamPageflowIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ProcessStateNode extends PageflowBasicNode<ProcessState> {

  public ProcessStateNode(@Nls String name, ProcessState identifyingElement) {
    super(identifyingElement, name);
  }

  @Override
  @NotNull
  public PageflowNodeType getNodeType() {
    return PageflowNodeType.PROCESS_STATE;
  }

  @Override
  public Icon getIcon() {
    return SeamPageflowIcons.ProcessState;
  }
}
