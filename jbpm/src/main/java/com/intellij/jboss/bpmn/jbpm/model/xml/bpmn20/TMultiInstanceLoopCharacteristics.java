package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tMultiInstanceLoopCharacteristics interface.
 */
public interface TMultiInstanceLoopCharacteristics extends Bpmn20DomElement, TLoopCharacteristics {

  @NotNull
  GenericAttributeValue<Boolean> getIsSequential();

  @NotNull
  GenericAttributeValue<TMultiInstanceFlowCondition> getBehavior();


  @NotNull
  GenericAttributeValue<String> getOneBehaviorEventRef();


  @NotNull
  GenericAttributeValue<String> getNoneBehaviorEventRef();

  @NotNull
  GenericDomValue<String> getLoopCardinality();

  @NotNull
  GenericDomValue<String> getLoopDataInputRef();

  @NotNull
  GenericDomValue<String> getLoopDataOutputRef();

  @NotNull
  TDataInput getInputDataItem();

  @NotNull
  TDataOutput getOutputDataItem();

  @NotNull
  @SubTagList("complexBehaviorDefinition")
  List<TComplexBehaviorDefinition> getComplexBehaviorDefinitions();

  @SubTagList("complexBehaviorDefinition")
  TComplexBehaviorDefinition addComplexBehaviorDefinition();

  @NotNull
  GenericDomValue<String> getCompletionCondition();
}
