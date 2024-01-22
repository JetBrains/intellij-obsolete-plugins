package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tInputOutputBinding interface.
 */
public interface TInputOutputBinding extends Bpmn20DomElement, TBaseElement {

  @NotNull
  @Required
  GenericAttributeValue<String> getOperationRef();


  @NotNull
  @Required
  GenericAttributeValue<String> getInputDataRef();

  @NotNull
  @Required
  GenericAttributeValue<String> getOutputDataRef();
}
