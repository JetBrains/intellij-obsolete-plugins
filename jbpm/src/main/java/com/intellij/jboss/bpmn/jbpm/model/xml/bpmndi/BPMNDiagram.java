// Generated on Tue Jun 12 14:10:06 CEST 2012
// DTD/Schema  :    http://www.omg.org/spec/BPMN/20100524/DI

package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi;

import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/DI:BPMNDiagram interface.
 */
public interface BPMNDiagram extends BpmndiDomElement, Diagram {

  @NotNull
  @Required
  BPMNPlane getBPMNPlane();

  @NotNull
  BPMNPlane addBPMNPlane();


  @NotNull
  List<BPMNLabelStyle> getBPMNLabelStyles();
}
