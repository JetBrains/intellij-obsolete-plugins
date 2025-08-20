package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tSubProcess interface.
 */
public interface TSubProcess extends FlowElementExplicitOwner, TActivity {

  @NotNull
  GenericAttributeValue<Boolean> getTriggeredByEvent();

  @NotNull
  @SubTagList("laneSet")
  List<TLaneSet> getLaneSets();

  @NotNull
  List<TArtifact> getArtifacts();
}
