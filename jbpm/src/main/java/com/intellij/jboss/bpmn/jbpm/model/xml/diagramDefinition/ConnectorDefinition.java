package com.intellij.jboss.bpmn.jbpm.model.xml.diagramDefinition;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.com/dd/1.0.0:ConnectorDefinition interface.
 */
public interface ConnectorDefinition extends DiagramDefinitionDomElement, ViewDefinition {

  @NotNull
  @Required
  GenericAttributeValue<String> getSourceDefinition();

  @NotNull
  @Required
  GenericAttributeValue<String> getTargetDefinition();
}
