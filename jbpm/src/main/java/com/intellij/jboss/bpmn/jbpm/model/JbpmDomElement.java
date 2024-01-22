package com.intellij.jboss.bpmn.jbpm.model;

import com.intellij.util.xml.JavaNameStrategy;
import com.intellij.util.xml.NameStrategy;
import com.intellij.util.xml.NameStrategyForAttributes;

@NameStrategy(JavaNameStrategy.class)
@NameStrategyForAttributes(JavaNameStrategy.class)
public interface JbpmDomElement extends JbpmMarkerDomElement {
}
