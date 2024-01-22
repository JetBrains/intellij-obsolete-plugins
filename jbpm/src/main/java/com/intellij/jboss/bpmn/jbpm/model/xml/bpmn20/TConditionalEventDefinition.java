package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefinitionKind;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tConditionalEventDefinition interface.
 */

@DefinitionKind("Conditional")
@DefaultNamePrefix("ConditionalEventDefinition")
public interface TConditionalEventDefinition extends Bpmn20DomElement, TEventDefinition {
  @NotNull
  @Required
  GenericDomValue<String> getCondition();
}
