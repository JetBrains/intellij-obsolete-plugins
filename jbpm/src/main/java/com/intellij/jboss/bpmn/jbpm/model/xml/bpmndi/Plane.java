// Generated on Tue Jun 12 14:11:29 CEST 2012
// DTD/Schema  :    http://www.omg.org/spec/DD/20100524/DI

package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi;

import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/DD/20100524/DI:Plane interface.
 */
public interface Plane extends Node {

  @NotNull
  @SubTagList("BPMNShape")
  List<BPMNShape> getBPMNShapes();

  @NotNull
  List<BPMNEdge> getBPMNEdges();

  @NotNull
  BPMNEdge addBPMNEdge();

  @NotNull
  List<BPMNLabel> getBPMNLabels();

  @NotNull
  List<BPMNPlane> getBPMNPlanes();
}
