package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tStandardLoopCharacteristics interface.
 */
public interface TStandardLoopCharacteristics extends Bpmn20DomElement, TLoopCharacteristics {
  @NotNull
  GenericAttributeValue<Boolean> getTestBefore();

  @NotNull
  GenericAttributeValue<Integer> getLoopMaximum();

  @NotNull
  GenericDomValue<String> getLoopCondition();
}
