// Generated on Tue Jun 12 14:10:06 CEST 2012
// DTD/Schema  :    http://www.omg.org/spec/BPMN/20100524/DI

package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi;

import com.intellij.jboss.bpmn.jbpm.constants.JbpmNamespaceConstants;
import com.intellij.jboss.bpmn.jbpm.model.converters.TBaseElementConverter;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TBaseElement;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/DI:BPMNShape interface.
 */
@Namespace(JbpmNamespaceConstants.JBPM_20_DI_NAMESPACE_KEY)
public interface BPMNShape extends LabeledShape {

  @NotNull
  @Convert(TBaseElementConverter.AnyBaseElementConverter.class)
  GenericAttributeValue<TBaseElement> getBpmnElement();

  @NotNull
  GenericAttributeValue<Boolean> getIsHorizontal();

  @NotNull
  GenericAttributeValue<Boolean> getIsExpanded();

  @NotNull
  GenericAttributeValue<Boolean> getIsMarkerVisible();

  @NotNull
  GenericAttributeValue<Boolean> getIsMessageVisible();

  @NotNull
  GenericAttributeValue<ParticipantBandKind> getParticipantBandKind();

  @NotNull
  GenericAttributeValue<String> getChoreographyActivityShape();

  @NotNull
  BPMNLabel getBPMNLabel();
}
