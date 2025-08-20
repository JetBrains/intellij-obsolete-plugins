// Generated on Tue Jun 12 14:11:29 CEST 2012
// DTD/Schema  :    http://www.omg.org/spec/DD/20100524/DI

package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi;

import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc.Point;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/DD/20100524/DI:Edge interface.
 */
public interface Edge extends DiagramElement {
  @NotNull
  Extension getExtension();

  @NotNull
  List<Point> getWaypoints();

  @SubTagList(value = "waypoint")
  Point addWaypoint();
}
