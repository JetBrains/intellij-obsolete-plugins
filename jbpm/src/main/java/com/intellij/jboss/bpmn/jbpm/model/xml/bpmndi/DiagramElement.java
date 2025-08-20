// Generated on Tue Jun 12 14:11:29 CEST 2012
// DTD/Schema  :    http://www.omg.org/spec/DD/20100524/DI

package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi;

import com.intellij.jboss.bpmn.jbpm.constants.JbpmNamespaceConstants;
import com.intellij.jboss.bpmn.jbpm.model.JbpmMarkerDomElement;
import com.intellij.jboss.bpmn.jbpm.providers.AsIsNameStrategy;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/DD/20100524/DI:DiagramElement interface.
 */
@Namespace(JbpmNamespaceConstants.OMG_DI_NAMESPACE_KEY)
@NameStrategy(AsIsNameStrategy.class)
@NameStrategyForAttributes(AsIsNameStrategy.class)
public interface DiagramElement extends JbpmMarkerDomElement {
  @NotNull
  @NameValue
  GenericAttributeValue<String> getId();
}
