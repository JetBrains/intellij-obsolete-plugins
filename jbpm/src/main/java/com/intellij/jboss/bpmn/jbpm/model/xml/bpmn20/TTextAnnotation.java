package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tTextAnnotation interface.
 */
public interface TTextAnnotation extends Bpmn20DomElement, TArtifact {
  @NotNull
  GenericAttributeValue<String> getTextFormat();

  @NotNull
  GenericDomValue<String> getText();
}
