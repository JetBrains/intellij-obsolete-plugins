// Generated on Mon Jun 11 15:20:55 CEST 2012
// DTD/Schema  :    http://www.omg.com/di/1.0.0

package com.intellij.jboss.bpmn.jbpm.model.xml.diagramInterchange;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.com/di/1.0.0:View interface.
 */
public interface View extends DiagramInterchangeDomElement {

  @NotNull
  @Required
  GenericAttributeValue<String> getId();


  @NotNull
  GenericAttributeValue<String> getContext();

  @NotNull
  @Required
  GenericAttributeValue<String> getDefinition();

  @NotNull
  GenericAttributeValue<String> getTargetConnector();

  @NotNull
  GenericAttributeValue<String> getSourceConnector();

  @NotNull
  List<Style> getStyles();

  @NotNull
  List<Node> getChilds();
}
