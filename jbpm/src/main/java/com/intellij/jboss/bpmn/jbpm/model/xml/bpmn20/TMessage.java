package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tMessage interface.
 */
public interface TMessage extends Bpmn20DomElement, TRootElement {

  @NotNull
  GenericAttributeValue<String> getItemRef();
}
