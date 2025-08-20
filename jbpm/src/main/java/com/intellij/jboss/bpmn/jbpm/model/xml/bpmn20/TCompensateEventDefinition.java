package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefinitionKind;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tCompensateEventDefinition interface.
 */
@DefinitionKind("Compensation")
@DefaultNamePrefix("CompensateEventDefinition")
public interface TCompensateEventDefinition extends Bpmn20DomElement, TEventDefinition {
  @NotNull
  @Required
  GenericAttributeValue<Boolean> getWaitForCompletion();

  @NotNull
  GenericAttributeValue<String> getActivityRef();
}
