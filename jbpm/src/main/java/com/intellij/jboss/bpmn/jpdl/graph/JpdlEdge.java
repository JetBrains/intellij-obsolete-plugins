package com.intellij.jboss.bpmn.jpdl.graph;

import com.intellij.jboss.bpmn.jpdl.model.xml.Transition;
import org.jetbrains.annotations.NotNull;

public interface JpdlEdge {
  JpdlNode getSource();

  JpdlNode getTarget();

  String getName();

  @NotNull
  Transition getIdentifyingElement();

  // several JpdlEdge objects can be created for one transition
  // if exists more than one node with duplicated name(will be rendered as error)
  boolean isDuplicated();
}
