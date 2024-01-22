package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tFlowElement interface.
 */
public interface TFlowElement extends Bpmn20DomElement, TBaseElement, NameAttributedElement {

  @Override
  @NotNull
  GenericAttributeValue<String> getName();

  @NotNull
  TAuditing getAuditing();

  @NotNull
  TMonitoring getMonitoring();

  @NotNull
  @SubTagList("categoryValueRef")
  List<GenericDomValue<String>> getCategoryValueRefs();
}
