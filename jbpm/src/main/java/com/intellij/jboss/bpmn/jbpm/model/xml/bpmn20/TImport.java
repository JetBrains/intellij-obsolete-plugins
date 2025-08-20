package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tImport interface.
 */
public interface TImport extends Bpmn20DomElement {

  @NotNull
  @Required
  GenericAttributeValue<String> getNamespace();

  @NotNull
  @Required
  GenericAttributeValue<String> getLocation();

  @NotNull
  @Required
  GenericAttributeValue<String> getImportType();
}
