package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tResourceParameterBinding interface.
 */
public interface TResourceParameterBinding extends Bpmn20DomElement, TBaseElement {
  @NotNull
  @Required
  GenericAttributeValue<String> getParameterRef();

  @NotNull
  @Required
  GenericDomValue<String> getExpression();
}
