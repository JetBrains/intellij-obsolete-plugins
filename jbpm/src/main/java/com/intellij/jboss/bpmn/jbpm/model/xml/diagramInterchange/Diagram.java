// Generated on Mon Jun 11 15:20:55 CEST 2012
// DTD/Schema  :    http://www.omg.com/di/1.0.0

package com.intellij.jboss.bpmn.jbpm.model.xml.diagramInterchange;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.com/di/1.0.0:Diagram interface.
 */
public interface Diagram extends DiagramInterchangeDomElement, View {

  @NotNull
  List<Connector> getConnectors();
}
