package com.intellij.jboss.bpmn.jbpm.model.xml.diagramDefinition;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.com/dd/1.0.0:StyleDefinition interface.
 */
public interface StyleDefinition extends DiagramDefinitionDomElement, NamedElement {

  @NotNull
  GenericAttributeValue<String> getDefault();

  @NotNull
  GenericAttributeValue<Boolean> getInherited();

  @NotNull
  @Required
  GenericAttributeValue<String> getType();
}
