package com.intellij.jboss.bpmn.jbpm.chart.dnd;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jbpm.BpmnBundle;
import com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartDataModel;
import com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartNode;
import com.intellij.jboss.bpmn.jbpm.dnd.ChartDnDNodeDefinition;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.EventDefinitionExplicitOwner;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowElement;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowNode;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import javax.swing.*;

import static com.intellij.jboss.bpmn.jbpm.chart.dnd.BpmnDnDUtils.*;

public enum BpmnDnDNodeType implements ChartDnDNodeDefinition<TFlowElement, BpmnChartNode, BpmnChartDataModel> {

  StartEvents("Create.StartEvents", JbossJbpmIcons.Bpmn.Events.Start_16_Empty),

  StartCompensation("Create.Compensation", JbossJbpmIcons.Bpmn.Events.Start_16_Compensation, createStartEventFn,
                    createCompensateEventDefinitionFn),
  StartConditional("Create.Conditional", JbossJbpmIcons.Bpmn.Events.Start_16_Conditional, createStartEventFn,
                   createConditionalEventDefinitionFn),
  StartError("Create.Error", JbossJbpmIcons.Bpmn.Events.Start_16_Error, createStartEventFn, createErrorEventDefinitionFn),
  StartEscalation("Create.Escalation", JbossJbpmIcons.Bpmn.Events.Start_16_Escalation, createStartEventFn,
                  createEscalationEventDefinitionFn),
  Start("Create.Start", JbossJbpmIcons.Bpmn.Events.Start_16_Empty, createStartEventFn, null),
  StartMessage("Create.Message", JbossJbpmIcons.Bpmn.Events.Start_16_Message, createStartEventFn, createMessageEventDefinitionFn),
  StartSignal("Create.Signal", JbossJbpmIcons.Bpmn.Events.Start_16_Signal, createStartEventFn, createSignalEventDefinitionFn),
  StartTimer("Create.Timer", JbossJbpmIcons.Bpmn.Events.Start_16_Timer, createStartEventFn, createTimerEventDefinitionFn),

  IntermediateThrowEvents("Create.IntermediateThrowEvents", JbossJbpmIcons.Bpmn.Events.IntermediateThrow_16_Empty),

  IntermediateThrowCompensation("Create.Compensation", JbossJbpmIcons.Bpmn.Events.IntermediateThrow_16_Compensation,
                                createIntermediateThrowEventFn,
                                createCompensateEventDefinitionFn),
  IntermediateThrowEscalation("Create.Escalation", JbossJbpmIcons.Bpmn.Events.IntermediateThrow_16_Escalation,
                              createIntermediateThrowEventFn,
                              createEscalationEventDefinitionFn),
  IntermediateThrow("Create.Intermediate", JbossJbpmIcons.Bpmn.Events.IntermediateThrow_16_Empty, createIntermediateThrowEventFn, null),
  IntermediateThrowLink("Create.Link", JbossJbpmIcons.Bpmn.Events.IntermediateThrow_16_Link, createIntermediateThrowEventFn,
                        createLinkEventDefinitionFn),
  IntermediateThrowMessage("Create.Message", JbossJbpmIcons.Bpmn.Events.IntermediateThrow_16_Message, createIntermediateThrowEventFn,
                           createMessageEventDefinitionFn),
  IntermediateThrowSignal("Create.Signal", JbossJbpmIcons.Bpmn.Events.IntermediateThrow_16_Signal, createIntermediateThrowEventFn,
                          createSignalEventDefinitionFn),

  IntermediateCatchEvents("Create.IntermediateCatchEvents", JbossJbpmIcons.Bpmn.Events.IntermediateCatch_16_Empty),

  IntermediateCatchConditional("Create.Conditional", JbossJbpmIcons.Bpmn.Events.IntermediateCatch_16_Conditional,
                               createIntermediateCatchEventFn,
                               createConditionalEventDefinitionFn),
  IntermediateCatchLink("Create.Link", JbossJbpmIcons.Bpmn.Events.IntermediateCatch_16_Link, createIntermediateCatchEventFn,
                        createLinkEventDefinitionFn),
  IntermediateCatchMessage("Create.Message", JbossJbpmIcons.Bpmn.Events.IntermediateCatch_16_Message, createIntermediateCatchEventFn,
                           createMessageEventDefinitionFn),
  IntermediateCatchSignal("Create.Signal", JbossJbpmIcons.Bpmn.Events.IntermediateCatch_16_Signal, createIntermediateCatchEventFn,
                          createSignalEventDefinitionFn),
  IntermediateCatchTimer("Create.Timer", JbossJbpmIcons.Bpmn.Events.IntermediateCatch_16_Timer, createIntermediateCatchEventFn,
                         createTimerEventDefinitionFn),

  EndEvents("Create.EndEvents", JbossJbpmIcons.Bpmn.Events.End_16_Empty),

  EndCompensation("Create.Compensation", JbossJbpmIcons.Bpmn.Events.End_16_Compensation, createEndEventFn,
                  createCompensateEventDefinitionFn),
  End("Create.End", JbossJbpmIcons.Bpmn.Events.End_16_Empty, createEndEventFn, null),
  EndError("Create.Error", JbossJbpmIcons.Bpmn.Events.End_16_Error, createEndEventFn, createErrorEventDefinitionFn),
  EndEscalation("Create.Escalation", JbossJbpmIcons.Bpmn.Events.End_16_Escalation, createEndEventFn, createEscalationEventDefinitionFn),
  EndMessage("Create.Message", JbossJbpmIcons.Bpmn.Events.End_16_Message, createEndEventFn, createMessageEventDefinitionFn),
  EndSignal("Create.Signal", JbossJbpmIcons.Bpmn.Events.End_16_Signal, createEndEventFn, createSignalEventDefinitionFn),
  EndTerminate("Create.Terminate", JbossJbpmIcons.Bpmn.Events.End_16_Terminate, createEndEventFn, createTerminateEventDefinitionFn),

  Gateways("Create.Gateways", JbossJbpmIcons.Bpmn.Gateways.EventBased),

  GatewayDataBasedExclusive(
    "Create.DataBasedExclusive",
    JbossJbpmIcons.Bpmn.Gateways.DataBasedExclusive,
    model -> model.getProcess().addExclusiveGateway()),
  GatewayEventBased(
    "Create.EventBased",
    JbossJbpmIcons.Bpmn.Gateways.EventBased,
    model -> model.getProcess().addEventBasedGateway()),
  GatewayInclusive(
    "Create.Inclusive",
    JbossJbpmIcons.Bpmn.Gateways.Inclusive,
    model -> model.getProcess().addInclusiveGateway()),
  GatewayParallel(
    "Create.Parallel",
    JbossJbpmIcons.Bpmn.Gateways.Parallel,

    model -> model.getProcess().addParallelGateway()),
  Tasks("Create.Task", JbossJbpmIcons.Bpmn.Tasks.Task),
  ServiceTask("ServiceTask", JbossJbpmIcons.Bpmn.Tasks.ServiceTask, model -> model.getProcess().addServiceTask());

  private final boolean isLeaf;
  @NotNull private final String name;
  @NotNull private final Icon icon;
  @Nullable private final Function<BpmnChartDataModel, BpmnChartNode> createFunction;

  BpmnDnDNodeType(@NotNull @PropertyKey(resourceBundle = BpmnBundle.BUNDLE) String name,
                  @NotNull Icon icon,
                  @NotNull final Function<? super BpmnChartDataModel, ? extends TFlowNode> createFunction) {
    this.isLeaf = true;
    this.name = name;
    this.icon = icon;
    this.createFunction = model -> model.addElement(createIdAndName(model, createFunction.fun(model)));
  }


  BpmnDnDNodeType(@NotNull @PropertyKey(resourceBundle = BpmnBundle.BUNDLE) String name,
                  @NotNull Icon icon,
                  @NotNull final CreateEventFn createEventFn,
                  @Nullable final CreateEventDefinitionFn createEventDefinitionFn) {
    this.isLeaf = true;
    this.name = name;
    this.icon = icon;
    this.createFunction = model -> {
      EventDefinitionExplicitOwner eventDefinitionExplicitOwner = createEventFn.fun(model);
      return model.addElement(createIdsAndNames(
        model,
        eventDefinitionExplicitOwner,
        createEventDefinitionFn == null ? null : createEventDefinitionFn.fun(eventDefinitionExplicitOwner)));
    };
  }

  BpmnDnDNodeType(@NotNull @PropertyKey(resourceBundle = BpmnBundle.BUNDLE) String name,
                  @NotNull Icon icon) {
    this.isLeaf = false;
    this.name = name;
    this.icon = icon;
    this.createFunction = null;
  }

  @Override
  @NotNull
  public String getName() {
    return BpmnBundle.message(name);
  }

  @Override
  @NotNull
  public Icon getIcon() {
    return icon;
  }

  @Nullable
  @Override
  public Function<BpmnChartDataModel, BpmnChartNode> getCreateFunction() {
    return createFunction;
  }


  @Override
  public boolean isLeafNode() {
    return isLeaf;
  }
}
