package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tGlobalScriptTask interface.
 */
public interface TGlobalScriptTask extends Bpmn20DomElement, TGlobalTask {
  @NotNull
  GenericAttributeValue<String> getScriptLanguage();

  @NotNull
  GenericDomValue<String> getScript();
}
