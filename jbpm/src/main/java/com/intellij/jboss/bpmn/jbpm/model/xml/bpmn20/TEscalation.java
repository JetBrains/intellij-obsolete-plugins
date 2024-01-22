package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tEscalation interface.
 */
public interface TEscalation extends Bpmn20DomElement, TRootElement {
  @NotNull
  GenericAttributeValue<String> getEscalationCode();

  @NotNull
  GenericAttributeValue<String> getStructureRef();
}
