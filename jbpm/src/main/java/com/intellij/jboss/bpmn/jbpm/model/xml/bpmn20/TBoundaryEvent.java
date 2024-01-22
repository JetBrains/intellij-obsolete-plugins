package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tBoundaryEvent interface.
 */
@DefaultNamePrefix("Boundary Event")
public interface TBoundaryEvent extends Bpmn20DomElement, TCatchEvent {

  @NotNull
  GenericAttributeValue<Boolean> getCancelActivity();

  @NotNull
  @Required
  GenericAttributeValue<String> getAttachedToRef();
}
