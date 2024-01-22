package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tItemDefinition interface.
 */
public interface TItemDefinition extends Bpmn20DomElement, TRootElement {

  @NotNull
  GenericAttributeValue<String> getStructureRef();

  @NotNull
  GenericAttributeValue<Boolean> getIsCollection();

  @NotNull
  GenericAttributeValue<TItemKind> getItemKind();
}
