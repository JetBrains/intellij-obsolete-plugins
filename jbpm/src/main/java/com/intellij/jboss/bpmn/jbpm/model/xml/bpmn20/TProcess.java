package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tProcess interface.
 */
public interface TProcess extends FlowElementExplicitOwner, TCallableElement {

  @SubTagList("sequenceFlow")
  List<TSequenceFlow> getSequenceFlows();

  @SubTagList("sequenceFlow")
  TSequenceFlow addSequenceFlow();

  @NotNull
  GenericAttributeValue<PsiClass> getFoo();

  @NotNull
  GenericAttributeValue<String> getProcessType();

  @NotNull
  GenericAttributeValue<Boolean> getIsClosed();

  @NotNull
  GenericAttributeValue<Boolean> getIsExecutable();

  @NotNull
  GenericAttributeValue<String> getDefinitionalCollaborationRef();

  @NotNull
  TAuditing getAuditing();

  @NotNull
  TMonitoring getMonitoring();

  @NotNull
  List<TProperty> getProperties();

  @NotNull
  @SubTagList("laneSet")
  List<TLaneSet> getLaneSets();

  @NotNull
  List<TFlowElement> getFlowElements();

  @NotNull
  List<TFlowNode> getFlowNodes();

  @NotNull
  List<TArtifact> getArtifacts();

  @NotNull
  @SubTagList("resourceRole")
  List<TResourceRole> getResourceRoles();

  @NotNull
  @SubTagList("correlationSubscription")
  List<TCorrelationSubscription> getCorrelationSubscriptions();

  @NotNull
  List<GenericDomValue<String>> getSupportses();
}
