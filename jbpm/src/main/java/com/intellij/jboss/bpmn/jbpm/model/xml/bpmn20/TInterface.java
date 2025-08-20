package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tInterface interface.
 */
public interface TInterface extends Bpmn20DomElement, TRootElement {
  @NotNull
  GenericAttributeValue<String> getImplementationRef();

  @NotNull
  @Required
  List<TOperation> getOperations();
}
