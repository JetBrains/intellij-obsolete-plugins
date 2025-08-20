package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tDataInput interface.
 */
public interface TDataInput extends Bpmn20DomElement, TBaseElement {
  @NotNull
  GenericAttributeValue<String> getItemSubjectRef();

  @NotNull
  GenericAttributeValue<Boolean> getIsCollection();

  @NotNull
  TDataState getDataState();
}
