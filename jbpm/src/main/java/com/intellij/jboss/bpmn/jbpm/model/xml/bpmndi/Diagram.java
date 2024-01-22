// Generated on Tue Jun 12 14:11:29 CEST 2012
// DTD/Schema  :    http://www.omg.org/spec/DD/20100524/DI

package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi;

import com.intellij.jboss.bpmn.jbpm.model.converters.DoubleConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/DD/20100524/DI:Diagram interface.
 */
public interface Diagram extends BpmndiDomElement {

  @NotNull
  GenericAttributeValue<String> getName();

  @NotNull
  GenericAttributeValue<String> getDocumentation();

  @NotNull
  @Convert(DoubleConverter.class)
  GenericAttributeValue<Double> getResolution();
}
