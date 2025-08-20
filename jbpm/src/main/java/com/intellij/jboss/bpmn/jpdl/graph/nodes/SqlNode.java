package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.Sql;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SqlNode extends JpdlBasicNode<Sql> {

  public SqlNode(Sql identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.SQL;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Sql;
  }
}
