package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tComplexBehaviorDefinition interface.
 */
public interface TComplexBehaviorDefinition extends Bpmn20DomElement, TBaseElement {

  @NotNull
  @Required
  TFormalExpression getCondition();

  @NotNull
  TImplicitThrowEvent getEvent();
}
