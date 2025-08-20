package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tMessageFlow interface.
 */
public interface TMessageFlow extends Bpmn20DomElement, TBaseElement {

  @NotNull
  @Required
  @Convert(MessageFlowRefConvertor.class)
  GenericAttributeValue<String> getSourceRef();

  @NotNull
  @Required
  @Convert(MessageFlowRefConvertor.class)
  GenericAttributeValue<String> getTargetRef();

  @NotNull
  GenericAttributeValue<String> getMessageRef();
}
