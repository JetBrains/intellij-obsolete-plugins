package com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc;

import com.intellij.jboss.bpmn.jbpm.constants.JbpmNamespaceConstants;
import com.intellij.jboss.bpmn.jbpm.model.JbpmDomElement;
import com.intellij.jboss.bpmn.jbpm.providers.AsIsNameStrategy;
import com.intellij.util.xml.NameStrategy;
import com.intellij.util.xml.NameStrategyForAttributes;
import com.intellij.util.xml.Namespace;

@Namespace(JbpmNamespaceConstants.OMG_DC_NAMESPACE_KEY)
@NameStrategy(AsIsNameStrategy.class)
@NameStrategyForAttributes(AsIsNameStrategy.class)
public interface BpmndcDomElement extends JbpmDomElement {
}
