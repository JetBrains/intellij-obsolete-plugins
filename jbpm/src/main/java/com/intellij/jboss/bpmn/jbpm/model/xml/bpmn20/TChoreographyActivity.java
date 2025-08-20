package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tChoreographyActivity interface.
 */
public interface TChoreographyActivity extends Bpmn20DomElement, TFlowNode {

  @NotNull
  @Required
  GenericAttributeValue<String> getInitiatingParticipantRef();

  @NotNull
  GenericAttributeValue<TChoreographyLoop> getLoopType();

  @NotNull
  @SubTagList("participantRef")
  @Required
  List<GenericDomValue<String>> getParticipantRefs();


  @NotNull
  @SubTagList("correlationKey")
  List<TCorrelationKey> getCorrelationKeys();
}
