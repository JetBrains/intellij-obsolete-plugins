package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tRelationship interface.
 */
public interface TRelationship extends Bpmn20DomElement, TBaseElement {

  @NotNull
  @Required
  GenericAttributeValue<String> getType();

  @NotNull
  GenericAttributeValue<TRelationshipDirection> getDirection();

  @NotNull
  @Required
  List<GenericDomValue<String>> getSources();

  @NotNull
  @Required
  List<GenericDomValue<String>> getTargets();
}
