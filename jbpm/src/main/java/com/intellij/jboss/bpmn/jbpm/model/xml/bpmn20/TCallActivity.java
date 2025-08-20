package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tCallActivity interface.
 */
public interface TCallActivity extends Bpmn20DomElement, TActivity {

  @NotNull
  GenericAttributeValue<String> getCalledElement();
}
