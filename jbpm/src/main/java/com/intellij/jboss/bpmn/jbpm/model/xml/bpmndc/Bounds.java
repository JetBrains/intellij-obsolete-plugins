package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc;

import com.intellij.jboss.bpmn.jbpm.model.converters.DoubleConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/DD/20100524/DC:Bounds interface.
 */
public interface Bounds extends BpmndcDomElement {

  @NotNull
  @Required
  @Convert(DoubleConverter.class)
  GenericAttributeValue<Double> getX();

  @NotNull
  @Required
  @Convert(DoubleConverter.class)
  GenericAttributeValue<Double> getY();

  @NotNull
  @Required
  @Convert(DoubleConverter.class)
  GenericAttributeValue<Double> getWidth();

  @NotNull
  @Required
  @Convert(DoubleConverter.class)
  GenericAttributeValue<Double> getHeight();
}
