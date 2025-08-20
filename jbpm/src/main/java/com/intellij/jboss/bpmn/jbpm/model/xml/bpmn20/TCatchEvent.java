package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tCatchEvent interface.
 */
public interface TCatchEvent extends EventDefinitionExplicitOwner, Bpmn20DomElement, TEvent {

  @NotNull
  GenericAttributeValue<Boolean> getParallelMultiple();

  @NotNull
  @SubTagList("dataOutput")
  List<TDataOutput> getDataOutputs();

  @NotNull
  @SubTagList("dataOutputAssociation")
  List<TDataOutputAssociation> getDataOutputAssociations();

  @NotNull
  TOutputSet getOutputSet();

  @Override
  @NotNull
  @SubTagList("eventDefinition")
  List<TEventDefinition> getEventDefinitions();

  @NotNull
  @SubTagList("eventDefinitionRef")
  List<GenericDomValue<String>> getEventDefinitionRefs();
}
