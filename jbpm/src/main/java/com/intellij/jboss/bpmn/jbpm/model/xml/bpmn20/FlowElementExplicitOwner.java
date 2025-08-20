package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.SubTagList;

import java.util.List;

public interface FlowElementExplicitOwner extends Bpmn20DomElement {
  @SubTagList("activity")
  List<TActivity> getActivities();

  @SubTagList("choreographyActivity")
  List<TChoreographyActivity> getChoreographyActivities();

  @SubTagList("event")
  List<TEvent> getEvents();

  @SubTagList("gateway")
  List<TGateway> getGateways();

  @SubTagList("inclusiveGateway")
  List<TInclusiveGateway> getInclusiveGateways();

  @SubTagList("inclusiveGateway")
  TInclusiveGateway addInclusiveGateway();

  @SubTagList("exclusiveGateway")
  List<TExclusiveGateway> getExclusiveGateways();

  @SubTagList("exclusiveGateway")
  TExclusiveGateway addExclusiveGateway();

  @SubTagList("eventBasedGateway")
  List<TEventBasedGateway> getEventBasedGateways();

  @SubTagList("eventBasedGateway")
  TEventBasedGateway addEventBasedGateway();

  @SubTagList("complexGateway")
  List<TComplexGateway> getComplexGateways();

  @SubTagList("complexGateway")
  TComplexGateway addComplexGateway();

  @SubTagList("parallelGateway")
  List<TParallelGateway> getParallelGateways();

  @SubTagList("parallelGateway")
  TParallelGateway addParallelGateway();

  @SubTagList("catchEvent")
  List<TCatchEvent> getCatchEvents();

  @SubTagList("throwEvent")
  List<TThrowEvent> getThrowEvents();

  @SubTagList("endEvent")
  List<TEndEvent> getEndEvents();

  @SubTagList("endEvent")
  TEndEvent addEndEvent();

  @SubTagList("implicitThrowEvent")
  List<TImplicitThrowEvent> getImplicitThrowEvents();

  @SubTagList("intermediateThrowEvent")
  List<TIntermediateThrowEvent> getIntermediateThrowEvents();

  @SubTagList("intermediateThrowEvent")
  TIntermediateThrowEvent addIntermediateThrowEvent();

  @SubTagList("boundaryEvent")
  List<TBoundaryEvent> getBoundaryEvents();

  @SubTagList("intermediateCatchEvent")
  List<TIntermediateCatchEvent> getIntermediateCatchEvents();

  @SubTagList("intermediateCatchEvent")
  TIntermediateCatchEvent addIntermediateCatchEvent();

  @SubTagList("startEvent")
  List<TStartEvent> getStartEvents();

  @SubTagList("startEvent")
  TStartEvent addStartEvent();

  @SubTagList("subProcess")
  List<TSubProcess> getSubProcesses();

  @SubTagList("callActivity")
  List<TCallActivity> getCallActivities();

  @SubTagList("task")
  List<TTask> getTasks();

  @SubTagList("receiveTask")
  List<TReceiveTask> getReceiveTasks();

  @SubTagList("manualTask")
  List<TManualTask> getManualTasks();

  @SubTagList("scriptTask")
  List<TScriptTask> getScriptTasks();

  @SubTagList("businessRuleTask")
  List<TBusinessRuleTask> getBusinessRuleTasks();

  @SubTagList("userTask")
  List<TUserTask> getUserTasks();

  @SubTagList("serviceTask")
  List<TServiceTask> getServiceTasks();

  @SubTagList("serviceTask")
  TServiceTask addServiceTask();

  @SubTagList("sendTask")
  List<TSendTask> getSendTasks();

  @SubTagList("adHocSubProcess")
  List<TAdHocSubProcess> getAdHocSubProcesses();

  @SubTagList("transaction")
  List<TTransaction> getTransactions();

  @SubTagList("choreographyTask")
  List<TChoreographyTask> getChoreographyTasks();

  @SubTagList("subChoreography")
  List<TSubChoreography> getSubChoreographies();

  @SubTagList("callChoreography")
  List<TCallChoreography> getCallChoreographies();
}
