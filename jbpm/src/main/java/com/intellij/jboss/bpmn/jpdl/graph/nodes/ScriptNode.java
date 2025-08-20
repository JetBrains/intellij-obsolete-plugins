package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.Script;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ScriptNode extends JpdlBasicNode<Script> {

  public ScriptNode(Script identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.SCRIPT;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Script;
  }
}
