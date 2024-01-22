package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tExtension interface.
 */
public interface TExtension extends Bpmn20DomElement {

  @NotNull
  GenericAttributeValue<String> getDefinition();

  @NotNull
  GenericAttributeValue<Boolean> getMustUnderstand();
}
