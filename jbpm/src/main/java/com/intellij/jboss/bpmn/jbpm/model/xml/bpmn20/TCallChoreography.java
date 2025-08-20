package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tCallChoreography interface.
 */
public interface TCallChoreography extends Bpmn20DomElement, TChoreographyActivity {

  @NotNull
  GenericAttributeValue<String> getCalledChoreographyRef();

  @NotNull
  @SubTagList("participantAssociation")
  List<TParticipantAssociation> getParticipantAssociations();

  @SubTagList("participantAssociation")
  TParticipantAssociation addParticipantAssociation();
}
