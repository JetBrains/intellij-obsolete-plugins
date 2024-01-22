package com.intellij.jboss.bpmn.jbpm.model.xml.diagramDefinition;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.com/dd/1.0.0:ChildDefinition interface.
 */
public interface ChildDefinition extends DiagramDefinitionDomElement, NamedElement {

  @NotNull
  GenericAttributeValue<Integer> getLowerBound();

  @NotNull
  GenericAttributeValue<Integer> getUpperBound();

  @NotNull
  @Required
  GenericAttributeValue<String> getTypeDefinition();
}
