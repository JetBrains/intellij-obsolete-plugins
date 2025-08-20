package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tAssignment interface.
 */
public interface TAssignment extends Bpmn20DomElement, TBaseElement {

  @NotNull
  @Required
  GenericDomValue<String> getFrom();

  @NotNull
  @Required
  GenericDomValue<String> getTo();
}
