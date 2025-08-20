package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tDataAssociation interface.
 */
public interface TDataAssociation extends Bpmn20DomElement, TBaseElement {

  @NotNull
  @SubTagList("sourceRef")
  List<GenericDomValue<String>> getSourceRefs();

  @NotNull
  @Required
  GenericDomValue<String> getTargetRef();

  @NotNull
  TFormalExpression getTransformation();

  @NotNull
  List<TAssignment> getAssignments();
}
