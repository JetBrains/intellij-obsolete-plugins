package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tCollaboration interface.
 */
public interface TCollaboration extends Bpmn20DomElement, TRootElement {

  @NotNull
  GenericAttributeValue<Boolean> getIsClosed();

  @NotNull
  List<TParticipant> getParticipants();

  @NotNull
  @SubTagList("messageFlow")
  List<TMessageFlow> getMessageFlows();

  @NotNull
  List<TArtifact> getArtifacts();

  @NotNull
  @SubTagList("conversationNode")
  List<TConversationNode> getConversationNodes();


  @NotNull
  @SubTagList("conversationAssociation")
  List<TConversationAssociation> getConversationAssociations();

  @NotNull
  @SubTagList("participantAssociation")
  List<TParticipantAssociation> getParticipantAssociations();


  @NotNull
  @SubTagList("messageFlowAssociation")
  List<TMessageFlowAssociation> getMessageFlowAssociations();


  @NotNull
  @SubTagList("correlationKey")
  List<TCorrelationKey> getCorrelationKeys();

  @NotNull
  @SubTagList("choreographyRef")
  List<GenericDomValue<String>> getChoreographyRefs();

  @NotNull
  @SubTagList("conversationLink")
  List<TConversationLink> getConversationLinks();
}
