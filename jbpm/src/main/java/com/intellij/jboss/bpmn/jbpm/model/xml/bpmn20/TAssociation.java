package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tAssociation interface.
 */
public interface TAssociation extends Bpmn20DomElement, TArtifact {

  @NotNull
  @Required
  GenericAttributeValue<String> getSourceRef();

  @NotNull
  @Required
  GenericAttributeValue<String> getTargetRef();

  @NotNull
  GenericAttributeValue<TAssociationDirection> getAssociationDirection();
}
