// Generated on Tue Jun 12 14:11:29 CEST 2012
// DTD/Schema  :    http://www.omg.org/spec/DD/20100524/DI

package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi;

import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc.Bounds;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.SubTag;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/DD/20100524/DI:Shape interface.
 */
public interface Shape extends Node {
  @NotNull
  @Required
  @SubTag("Bounds")
  Bounds getBounds();
}