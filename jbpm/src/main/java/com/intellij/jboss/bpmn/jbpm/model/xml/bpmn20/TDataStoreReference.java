package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tDataStoreReference interface.
 */
public interface TDataStoreReference extends Bpmn20DomElement, TFlowElement {

  @NotNull
  GenericAttributeValue<String> getItemSubjectRef();

  @NotNull
  GenericAttributeValue<String> getDataStoreRef();

  @NotNull
  TDataState getDataState();
}
