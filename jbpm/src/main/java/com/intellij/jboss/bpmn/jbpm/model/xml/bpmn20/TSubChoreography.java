package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tSubChoreography interface.
 */
public interface TSubChoreography extends FlowElementExplicitOwner, TChoreographyActivity {
  @NotNull
  @SubTagList("flowElement")
  List<TFlowElement> getFlowElements();

  @NotNull
  List<TArtifact> getArtifacts();
}
