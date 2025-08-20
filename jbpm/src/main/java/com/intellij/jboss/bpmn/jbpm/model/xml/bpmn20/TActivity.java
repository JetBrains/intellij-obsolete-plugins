package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tActivity interface.
 */
public interface TActivity extends Bpmn20DomElement, TFlowNode {

  @NotNull
  GenericAttributeValue<Boolean> getIsForCompensation();

  @NotNull
  GenericAttributeValue<Integer> getStartQuantity();

  @NotNull
  GenericAttributeValue<Integer> getCompletionQuantity();

  @NotNull
  GenericAttributeValue<String> getDefault();

  @NotNull
  TInputOutputSpecification getIoSpecification();

  @NotNull
  List<TProperty> getProperties();

  @NotNull
  @SubTagList("dataInputAssociation")
  List<TDataInputAssociation> getDataInputAssociations();


  @NotNull
  @SubTagList("dataOutputAssociation")
  List<TDataOutputAssociation> getDataOutputAssociations();

  @NotNull
  @SubTagList("resourceRole")
  List<TResourceRole> getResourceRoles();

  @NotNull
  TLoopCharacteristics getLoopCharacteristics();
}
