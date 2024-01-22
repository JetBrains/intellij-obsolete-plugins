package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.Rules;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RulesNode extends JpdlBasicNode<Rules> {

  public RulesNode(Rules identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.RULES;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Rule;
  }
}
