package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc;

import com.intellij.jboss.bpmn.jbpm.model.converters.DoubleConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/DD/20100524/DC:Font interface.
 */
public interface Font extends BpmndcDomElement {

  @NotNull
  GenericAttributeValue<String> getName();

  @NotNull
  @Convert(DoubleConverter.class)
  GenericAttributeValue<Double> getSize();

  @NotNull
  GenericAttributeValue<Boolean> getIsBold();

  @NotNull
  GenericAttributeValue<Boolean> getIsItalic();

  @NotNull
  GenericAttributeValue<Boolean> getIsUnderline();

  @NotNull
  GenericAttributeValue<Boolean> getIsStrikeThrough();
}
