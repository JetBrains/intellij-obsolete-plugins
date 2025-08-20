package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.SubProcess;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SubProcessNode extends JpdlBasicNode<SubProcess> {

  public SubProcessNode(SubProcess identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.SUBPROCESS;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.SubProcess;
  }
}
