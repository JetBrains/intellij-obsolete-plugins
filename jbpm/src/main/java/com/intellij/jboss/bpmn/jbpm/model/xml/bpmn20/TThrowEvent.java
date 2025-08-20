package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tThrowEvent interface.
 */
public interface TThrowEvent extends EventDefinitionExplicitOwner, Bpmn20DomElement, TEvent {
  @NotNull
  @SubTagList("dataInput")
  List<TDataInput> getDataInputs();

  @NotNull
  @SubTagList("dataInputAssociation")
  List<TDataInputAssociation> getDataInputAssociations();

  @NotNull
  TInputSet getInputSet();

  @Override
  @NotNull
  @SubTagList("eventDefinition")
  List<TEventDefinition> getEventDefinitions();

  @NotNull
  @SubTagList("eventDefinitionRef")
  List<GenericDomValue<String>> getEventDefinitionRefs();
}
