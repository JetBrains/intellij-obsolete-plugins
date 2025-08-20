package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tAdHocSubProcess interface.
 */
public interface TAdHocSubProcess extends Bpmn20DomElement, TSubProcess {

  @NotNull
  GenericAttributeValue<Boolean> getCancelRemainingInstances();

  @NotNull
  GenericAttributeValue<TAdHocOrdering> getOrdering();


  @NotNull
  GenericDomValue<String> getCompletionCondition();
}
