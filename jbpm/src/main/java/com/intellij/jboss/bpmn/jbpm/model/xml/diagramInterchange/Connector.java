// Generated on Mon Jun 11 15:20:55 CEST 2012
// DTD/Schema  :    http://www.omg.com/di/1.0.0

package com.intellij.jboss.bpmn.jbpm.model.xml.diagramInterchange;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.com/di/1.0.0:Connector interface.
 */
public interface Connector extends DiagramInterchangeDomElement, View {

  @NotNull
  @Required
  GenericAttributeValue<String> getSource();

  @NotNull
  @Required
  GenericAttributeValue<String> getTarget();

  @NotNull
  List<Bendpoint> getBendpoints();
}
