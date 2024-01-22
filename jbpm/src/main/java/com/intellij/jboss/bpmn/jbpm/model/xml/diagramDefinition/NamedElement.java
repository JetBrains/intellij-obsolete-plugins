package com.intellij.jboss.bpmn.jbpm.model.xml.diagramDefinition;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.com/dd/1.0.0:NamedElement interface.
 */
public interface NamedElement extends DiagramDefinitionDomElement {

  @NotNull
  @Required
  GenericAttributeValue<String> getName();
}
