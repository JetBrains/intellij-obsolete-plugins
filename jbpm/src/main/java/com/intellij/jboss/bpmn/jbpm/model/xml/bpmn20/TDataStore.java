package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tDataStore interface.
 */
public interface TDataStore extends Bpmn20DomElement, TRootElement {

  @NotNull
  GenericAttributeValue<Integer> getCapacity();

  @NotNull
  GenericAttributeValue<Boolean> getIsUnlimited();

  @NotNull
  GenericAttributeValue<String> getItemSubjectRef();

  @NotNull
  TDataState getDataState();
}
