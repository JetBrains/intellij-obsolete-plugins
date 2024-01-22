package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.End;
import com.intellij.jboss.bpmn.jpdl.model.xml.EndCancel;
import com.intellij.jboss.bpmn.jpdl.model.xml.EndError;
import com.intellij.jboss.bpmn.jpdl.model.xml.JpdlNamedActivity;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class EndActivityNode<E extends JpdlNamedActivity> extends JpdlBasicNode<E> {

  public EndActivityNode(E identifyingElement) {
    super(identifyingElement);
  }

  public static class EndNode extends EndActivityNode<End> {
    public EndNode(End identifyingElement) {
      super(identifyingElement);
    }

    @Override
    @NotNull
    public JpdlNodeType getNodeType() {
      return JpdlNodeType.END;
    }

    @Override
    public Icon getIcon() {
      return JbossJbpmIcons.Jpdl.End;
    }
  }

  public static class EndCancelNode extends EndActivityNode<EndCancel> {
    public EndCancelNode(EndCancel identifyingElement) {
      super(identifyingElement);
    }

    @Override
    @NotNull
    public JpdlNodeType getNodeType() {
      return JpdlNodeType.END_CANCEL;
    }

    @Override
    public Icon getIcon() {
      return JbossJbpmIcons.Jpdl.End_cancel;
    }
  }

  public static class EndErrorNode extends EndActivityNode<EndError> {
    public EndErrorNode(EndError identifyingElement) {
      super(identifyingElement);
    }

    @Override
    @NotNull
    public JpdlNodeType getNodeType() {
      return JpdlNodeType.END_ERROR;
    }

    @Override
    public Icon getIcon() {
      return JbossJbpmIcons.Jpdl.End_error;
    }
  }
}
