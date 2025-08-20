package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tDataObjectReference interface.
 */
public interface TDataObjectReference extends Bpmn20DomElement, TFlowElement {

  @NotNull
  GenericAttributeValue<String> getItemSubjectRef();

  @NotNull
  GenericAttributeValue<String> getDataObjectRef();

  @NotNull
  TDataState getDataState();
}
