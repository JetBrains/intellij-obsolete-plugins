package com.intellij.jboss.bpmn.jbpm.model.xml.diagramDefinition;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.com/dd/1.0.0:DiagramDefinition interface.
 */
public interface DiagramDefinition extends DiagramDefinitionDomElement, ViewDefinition {


  /**
   * Returns the value of the name child.
   *
   * @return the value of the name child.
   */
  @Override
  @NotNull
  @Required
  GenericAttributeValue<String> getName();
}
