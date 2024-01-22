package com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers;

import com.intellij.jboss.bpmn.jbpm.diagram.BpmnDiagramPresentationConstants;
import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModel;
import com.intellij.jboss.bpmn.jbpm.model.JbpmDomElement;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TBaseElement;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowElement;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;


public class Bpmn20DomElementWrapper extends BpmnElementWrapper<TBaseElement> {

  public static final Key<Boolean> IS_START_STATE = Key.create("startAction");

  public Bpmn20DomElementWrapper(@NotNull JbpmDomElement element) {
    super(element.createStableCopy());
  }

  @NotNull
  @Override
  public String getName() {
    if (!isValid()) {
      return BpmnDiagramPresentationConstants.getLabelInvalid();
    }
    if (myElement instanceof TFlowElement) {
      @NlsSafe String stringValue = ((TFlowElement)myElement).getName().getStringValue();
      return StringUtil.notNullize(stringValue);
    }
    return "";
  }

  @Override
  public Icon getIcon() {
    if (!isValid()) {
      return PlatformIcons.ERROR_INTRODUCTION_ICON;
    }
    return myElement.getPresentation().getIcon();
  }

  @Override
  public String getFqn() {
    return isValid() ? myElement.toString() : BpmnDiagramPresentationConstants.getLabelInvalid();
  }

  @Override
  public boolean isValid() {
    return myElement.isValid();
  }

  @NotNull
  @Override
  public List<BpmnDomModel> getBpmnModels() {
    throw new UnsupportedOperationException(myElement.toString());
  }
}
