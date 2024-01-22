package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tResourceParameter interface.
 */
public interface TResourceParameter extends Bpmn20DomElement, TBaseElement {
  @NotNull
  GenericAttributeValue<String> getType();

  @NotNull
  GenericAttributeValue<Boolean> getIsRequired();
}
