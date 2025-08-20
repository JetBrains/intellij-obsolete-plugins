package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tCallConversation interface.
 */
public interface TCallConversation extends Bpmn20DomElement, TConversationNode {

  @NotNull
  GenericAttributeValue<String> getCalledCollaborationRef();

  @NotNull
  @SubTagList("participantAssociation")
  List<TParticipantAssociation> getParticipantAssociations();
}
