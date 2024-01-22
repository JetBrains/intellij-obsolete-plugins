package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tParticipantAssociation interface.
 */
public interface TParticipantAssociation extends Bpmn20DomElement, TBaseElement {

  @NotNull
  @Required
  GenericDomValue<String> getInnerParticipantRef();

  @NotNull
  @Required
  GenericDomValue<String> getOuterParticipantRef();
}
