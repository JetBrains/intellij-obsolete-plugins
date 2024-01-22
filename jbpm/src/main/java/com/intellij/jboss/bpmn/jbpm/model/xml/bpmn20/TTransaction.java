package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tTransaction interface.
 */
public interface TTransaction extends Bpmn20DomElement, TSubProcess {

  @NotNull
  GenericAttributeValue<String> getMethod();
}
