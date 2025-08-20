package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.RulesDecision;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RulesDecisionNode extends JpdlBasicNode<RulesDecision> {

  public RulesDecisionNode(RulesDecision identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.RULES_DECISION;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Rule_decision;
  }
}
