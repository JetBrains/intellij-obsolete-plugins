package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tBaseElementWithMixedContent interface.
 */
public interface TBaseElementWithMixedContent extends Bpmn20DomElement {

  @NotNull
  @Required
  String getValue();

  void setValue(@NotNull String value);

  TDocumentation addDocumentation();

  @NotNull
  TExtensionElements getExtensionElements();
}
