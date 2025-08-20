package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface EventDefinitionExplicitOwner extends Bpmn20DomElement, TEvent {
  @NotNull
  @SubTagList("eventDefinition")
  List<TEventDefinition> getEventDefinitions();

  @NotNull
  @SubTagList("cancelEventDefinition")
  List<TCancelEventDefinition> getCancelEventDefinitions();

  @NotNull
  @SubTagList("cancelEventDefinition")
  TCancelEventDefinition addCancelEventDefinition();

  @NotNull
  @SubTagList("compensateEventDefinition")
  List<TCompensateEventDefinition> getCompensateEventDefinitions();

  @NotNull
  @SubTagList("compensateEventDefinition")
  TCompensateEventDefinition addCompensateEventDefinition();

  @NotNull
  @SubTagList("conditionalEventDefinition")
  List<TConditionalEventDefinition> getConditionalEventDefinitions();

  @NotNull
  @SubTagList("conditionalEventDefinition")
  TConditionalEventDefinition addConditionalEventDefinition();

  @NotNull
  @SubTagList("errorEventDefinition")
  List<TErrorEventDefinition> getErrorEventDefinitions();

  @NotNull
  @SubTagList("errorEventDefinition")
  TErrorEventDefinition addErrorEventDefinition();

  @NotNull
  @SubTagList("escalationEventDefinition")
  List<TEscalationEventDefinition> getEscalationEventDefinitions();

  @NotNull
  @SubTagList("escalationEventDefinition")
  TEscalationEventDefinition addEscalationEventDefinition();

  @NotNull
  @SubTagList("linkEventDefinition")
  List<TLinkEventDefinition> getLinkEventDefinitions();

  @NotNull
  @SubTagList("linkEventDefinition")
  TLinkEventDefinition addLinkEventDefinition();

  @NotNull
  @SubTagList("messageEventDefinition")
  List<TMessageEventDefinition> getMessageEventDefinitions();

  @NotNull
  @SubTagList("messageEventDefinition")
  TMessageEventDefinition addMessageEventDefinition();

  @NotNull
  @SubTagList("signalEventDefinition")
  List<TSignalEventDefinition> getSignalEventDefinitions();

  @NotNull
  @SubTagList("signalEventDefinition")
  TSignalEventDefinition addSignalEventDefinition();

  @NotNull
  @SubTagList("terminateEventDefinition")
  List<TTerminateEventDefinition> getTerminateEventDefinitions();

  @NotNull
  @SubTagList("terminateEventDefinition")
  TTerminateEventDefinition addTerminateEventDefinition();

  @NotNull
  @SubTagList("timerEventDefinition")
  List<TTimerEventDefinition> getTimerEventDefinitions();

  @NotNull
  @SubTagList("timerEventDefinition")
  TTimerEventDefinition addTimerEventDefinition();
}
