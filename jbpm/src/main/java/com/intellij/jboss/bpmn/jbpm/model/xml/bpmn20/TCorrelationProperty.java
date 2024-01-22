package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tCorrelationProperty interface.
 */
public interface TCorrelationProperty extends Bpmn20DomElement, TRootElement {

  @NotNull
  GenericAttributeValue<String> getType();

  @NotNull
  @SubTagList("correlationPropertyRetrievalExpression")
  @Required
  List<TCorrelationPropertyRetrievalExpression> getCorrelationPropertyRetrievalExpressions();

  @SubTagList("correlationPropertyRetrievalExpression")
  TCorrelationPropertyRetrievalExpression addCorrelationPropertyRetrievalExpression();
}
