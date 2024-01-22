// Generated on Mon Jun 11 15:20:55 CEST 2012
// DTD/Schema  :    http://www.omg.com/di/1.0.0

package com.intellij.jboss.bpmn.jbpm.model.xml.diagramInterchange;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.com/di/1.0.0:Bendpoint interface.
 */
public interface Bendpoint extends DiagramInterchangeDomElement {

  @NotNull
  @Required
  GenericAttributeValue<Integer> getSourceX();

  @NotNull
  @Required
  GenericAttributeValue<Integer> getSourceY();

  @NotNull
  @Required
  GenericAttributeValue<Integer> getTargetX();

  @NotNull
  @Required
  GenericAttributeValue<Integer> getTargetY();
}
