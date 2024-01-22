package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tOperation interface.
 */
public interface TOperation extends Bpmn20DomElement, TBaseElement {

  @NotNull
  GenericAttributeValue<String> getImplementationRef();

  @NotNull
  @Required
  GenericDomValue<String> getInMessageRef();

  @NotNull
  GenericDomValue<String> getOutMessageRef();

  @NotNull
  @SubTagList("errorRef")
  List<GenericDomValue<String>> getErrorRefs();
}
