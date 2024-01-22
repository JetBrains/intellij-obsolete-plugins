package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

public interface NameAttributedElement {
  @NotNull
  GenericAttributeValue<String> getName();
}
