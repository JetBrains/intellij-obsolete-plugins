package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tCorrelationPropertyBinding interface.
 */
public interface TCorrelationPropertyBinding extends Bpmn20DomElement, TBaseElement {

  @NotNull
  @Required
  GenericAttributeValue<String> getCorrelationPropertyRef();

  @NotNull
  @Required
  TFormalExpression getDataPath();
}
