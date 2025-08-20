// Generated on Tue Jun 12 14:10:06 CEST 2012
// DTD/Schema  :    http://www.omg.org/spec/BPMN/20100524/DI

package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi;

import com.intellij.jboss.bpmn.jbpm.constants.JbpmNamespaceConstants;
import com.intellij.jboss.bpmn.jbpm.model.converters.BPMNPlaneElementConvertor;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TBaseElement;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/DI:BPMNPlane interface.
 */
@Namespace(JbpmNamespaceConstants.JBPM_20_DI_NAMESPACE_KEY)
public interface BPMNPlane extends Plane {
  @Convert(BPMNPlaneElementConvertor.class)
  @NotNull
  GenericAttributeValue<TBaseElement> getBpmnElement();

  @Override
  @NotNull
  @Required
  List<BPMNShape> getBPMNShapes();

  @NotNull
  BPMNShape addBPMNShape();
}
