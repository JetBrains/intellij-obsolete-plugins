package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc;

import com.intellij.jboss.bpmn.jbpm.constants.JbpmNamespaceConstants;
import com.intellij.jboss.bpmn.jbpm.model.converters.DoubleConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

@Namespace(JbpmNamespaceConstants.OMG_DI_NAMESPACE_KEY)
public interface Point extends BpmndcDomElement {

  @NotNull
  @Required
  @Convert(DoubleConverter.class)
  GenericAttributeValue<Double> getX();

  @NotNull
  @Required
  @Convert(DoubleConverter.class)
  GenericAttributeValue<Double> getY();
}
