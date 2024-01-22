package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tChoreography interface.
 */
public interface TChoreography extends FlowElementExplicitOwner, TCollaboration {
  @NotNull
  @SubTagList("flowElement")
  List<TFlowElement> getFlowElements();
}
