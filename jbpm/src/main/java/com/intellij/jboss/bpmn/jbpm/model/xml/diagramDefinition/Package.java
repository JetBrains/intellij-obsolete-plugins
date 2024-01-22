package com.intellij.jboss.bpmn.jbpm.model.xml.diagramDefinition;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.com/dd/1.0.0:Package interface.
 */
public interface Package extends DiagramDefinitionDomElement, NamedElement {

  @NotNull
  @Required
  GenericAttributeValue<String> getNsURI();

  @NotNull
  @Required
  GenericAttributeValue<String> getNsPrefix();


  @NotNull
  @SubTagList("viewDefinition")
  List<ViewDefinition> getViewDefinitions();
}
