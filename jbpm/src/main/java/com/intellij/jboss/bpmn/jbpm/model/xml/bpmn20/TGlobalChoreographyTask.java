package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tGlobalChoreographyTask interface.
 */
public interface TGlobalChoreographyTask extends Bpmn20DomElement, TChoreography {

  @NotNull
  GenericAttributeValue<String> getInitiatingParticipantRef();
}
