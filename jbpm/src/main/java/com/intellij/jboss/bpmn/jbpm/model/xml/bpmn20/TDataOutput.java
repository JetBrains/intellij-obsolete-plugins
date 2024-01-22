package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tDataOutput interface.
 */
public interface TDataOutput extends Bpmn20DomElement, TBaseElement {

  @NotNull
  GenericAttributeValue<String> getItemSubjectRef();

  @NotNull
  GenericAttributeValue<Boolean> getIsCollection();

  @NotNull
  TDataState getDataState();
}
