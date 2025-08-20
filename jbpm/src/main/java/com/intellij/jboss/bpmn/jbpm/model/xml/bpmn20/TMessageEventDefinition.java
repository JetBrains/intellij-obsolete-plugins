package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefinitionKind;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tMessageEventDefinition interface.
 */
@DefinitionKind("Message")
@DefaultNamePrefix("MessageEventDefinition")
public interface TMessageEventDefinition extends Bpmn20DomElement, TEventDefinition {
  @NotNull
  GenericAttributeValue<String> getMessageRef();

  @NotNull
  GenericDomValue<String> getOperationRef();
}
