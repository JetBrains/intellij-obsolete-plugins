package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tPerformer interface.
 */
public interface TPerformer extends Bpmn20DomElement, TResourceRole {
  @NotNull
  TResourceAssignmentExpression getResourceAssignmentExpression();

  @NotNull
  @Required
  GenericDomValue<String> getResourceRef();
}
