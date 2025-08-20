package com.intellij.jboss.bpmn.jbpm.model.xml.diagramDefinition;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.com/dd/1.0.0:ViewDefinition interface.
 */
public interface ViewDefinition extends DiagramDefinitionDomElement, NamedElement {

  @NotNull
  @Required
  GenericAttributeValue<String> getId();

  @NotNull
  GenericAttributeValue<Boolean> getAbstract();

  @NotNull
  GenericAttributeValue<String> getSuperDefinition();

  @NotNull
  GenericAttributeValue<String> getContextType();

  @NotNull
  List<Constraint> getConstraints();

  @NotNull
  @SubTagList("styleDefinition")
  List<StyleDefinition> getStyleDefinitions();

  @NotNull
  @SubTagList("childDefinition")
  List<ChildDefinition> getChildDefinitions();
}
