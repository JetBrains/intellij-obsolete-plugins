// Generated on Tue Jun 12 14:10:06 CEST 2012
// DTD/Schema  :    http://www.omg.org/spec/BPMN/20100524/DI

package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi;

import com.intellij.jboss.bpmn.jbpm.constants.JbpmNamespaceConstants;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc.Font;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/DI:BPMNLabelStyle interface.
 */
@Namespace(JbpmNamespaceConstants.JBPM_20_DI_NAMESPACE_KEY)
public interface BPMNLabelStyle extends BpmndiDomElement, Style {

  @NotNull
  @Required
  Font getFont();
}
