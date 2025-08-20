package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tParticipantMultiplicity interface.
 */
public interface TParticipantMultiplicity extends Bpmn20DomElement, TBaseElement {

  @NotNull
  GenericAttributeValue<Integer> getMinimum();

  @NotNull
  GenericAttributeValue<Integer> getMaximum();
}
