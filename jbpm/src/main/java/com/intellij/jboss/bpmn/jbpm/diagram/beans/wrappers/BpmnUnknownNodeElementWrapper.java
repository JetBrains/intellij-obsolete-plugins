package com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers;

import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModel;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class BpmnUnknownNodeElementWrapper extends BpmnElementWrapper<String> {

  public BpmnUnknownNodeElementWrapper(@NotNull String nodeId) {
    super(nodeId);
  }

  @SuppressWarnings("HardCodedStringLiteral")
  @NotNull
  @Override
  public String getName() {
    return getElement();
  }

  @Override
  public Icon getIcon() {
    return PlatformIcons.ERROR_INTRODUCTION_ICON;
  }

  @Override
  public String getFqn() {
    return myElement;
  }

  @NotNull
  @Override
  public List<BpmnDomModel> getBpmnModels() {
    throw new UnsupportedOperationException(myElement);
  }
}
