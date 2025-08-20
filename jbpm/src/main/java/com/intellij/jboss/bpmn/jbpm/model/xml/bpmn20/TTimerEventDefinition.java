package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefinitionKind;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tTimerEventDefinition interface.
 */
@DefinitionKind("Timer")
@DefaultNamePrefix("TimerEventDefinition")
public interface TTimerEventDefinition extends Bpmn20DomElement, TEventDefinition {

  @NotNull
  GenericDomValue<String> getTimeDate();

  @NotNull
  GenericDomValue<String> getTimeDuration();

  @NotNull
  GenericDomValue<String> getTimeCycle();
}
