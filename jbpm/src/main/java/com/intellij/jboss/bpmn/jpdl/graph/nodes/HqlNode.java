package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.Hql;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HqlNode extends JpdlBasicNode<Hql> {

  public HqlNode(Hql identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.HQL;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Hql;
  }
}
