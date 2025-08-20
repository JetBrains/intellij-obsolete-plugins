// Generated on Tue Jun 12 14:10:06 CEST 2012
// DTD/Schema  :    http://www.omg.org/spec/BPMN/20100524/DI

package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi;

import com.intellij.jboss.bpmn.jbpm.constants.JbpmNamespaceConstants;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/DI:BPMNEdge interface.
 */
@Namespace(JbpmNamespaceConstants.JBPM_20_DI_NAMESPACE_KEY)
public interface BPMNEdge extends LabeledEdge {

  @NotNull
  GenericAttributeValue<String> getBpmnElement();

  @NotNull
  GenericAttributeValue<String> getSourceElement();

  @NotNull
  GenericAttributeValue<String> getTargetElement();

  @NotNull
  GenericAttributeValue<MessageVisibleKind> getMessageVisibleKind();

  @NotNull
  BPMNLabel getBPMNLabel();
}
