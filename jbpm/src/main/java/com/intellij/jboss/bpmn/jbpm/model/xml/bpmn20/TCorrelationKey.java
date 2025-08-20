package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tCorrelationKey interface.
 */
public interface TCorrelationKey extends Bpmn20DomElement, TBaseElement {

  @NotNull
  @SubTagList("correlationPropertyRef")
  List<GenericDomValue<String>> getCorrelationPropertyRefs();

  @SubTagList("correlationPropertyRef")
  GenericDomValue<String> addCorrelationPropertyRef();
}
