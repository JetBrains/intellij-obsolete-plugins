package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tCorrelationSubscription interface.
 */
public interface TCorrelationSubscription extends Bpmn20DomElement, TBaseElement {

  @NotNull
  @Required
  GenericAttributeValue<String> getCorrelationKeyRef();

  @NotNull
  @SubTagList("correlationPropertyBinding")
  List<TCorrelationPropertyBinding> getCorrelationPropertyBindings();

  @SubTagList("correlationPropertyBinding")
  TCorrelationPropertyBinding addCorrelationPropertyBinding();
}
