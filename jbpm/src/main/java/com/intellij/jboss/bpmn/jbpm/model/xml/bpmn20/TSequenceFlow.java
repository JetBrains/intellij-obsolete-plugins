package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.converters.FlowElementConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tSequenceFlow interface.
 */
public interface TSequenceFlow extends Bpmn20DomElement, TFlowElement {

  @NotNull
  @Required
  @Convert(FlowElementConverter.class)
  GenericAttributeValue<TBaseElement> getSourceRef();

  @NotNull
  @Required
  @Convert(FlowElementConverter.class)
  GenericAttributeValue<TBaseElement> getTargetRef();

  @NotNull
  GenericAttributeValue<Boolean> getIsImmediate();

  @NotNull
  GenericDomValue<String> getConditionExpression();
}
