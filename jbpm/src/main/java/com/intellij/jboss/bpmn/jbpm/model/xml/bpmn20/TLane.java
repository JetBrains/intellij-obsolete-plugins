package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tLane interface.
 */
public interface TLane extends Bpmn20DomElement, TBaseElement {

  @NotNull
  GenericAttributeValue<String> getPartitionElementRef();

  @NotNull
  TBaseElement getPartitionElement();

  @NotNull
  @SubTagList("flowNodeRef")
  List<GenericDomValue<String>> getFlowNodeRefs();

  @NotNull
  TLaneSet getChildLaneSet();
}
