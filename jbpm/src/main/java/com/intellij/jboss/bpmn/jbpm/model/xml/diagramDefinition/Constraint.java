package com.intellij.jboss.bpmn.jbpm.model.xml.diagramDefinition;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.com/dd/1.0.0:Constraint interface.
 */
public interface Constraint extends DiagramDefinitionDomElement, NamedElement {

  @NotNull
  @Required
  GenericAttributeValue<String> getCondition();
}
