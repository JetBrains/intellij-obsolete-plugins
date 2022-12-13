package com.intellij.seam.pageflow.graph;

import com.intellij.seam.pageflow.model.xml.pageflow.Transition;
import org.jetbrains.annotations.NotNull;

public interface PageflowEdge {
  PageflowNode getSource();

  PageflowNode getTarget();

  String getName();

  @NotNull
  Transition getIdentifyingElement();

  // several PageflowEdge objects can be created for one transition
  // if exists more than one node with duplicated name(will be rendered as error)
  boolean isDuplicated();
}
