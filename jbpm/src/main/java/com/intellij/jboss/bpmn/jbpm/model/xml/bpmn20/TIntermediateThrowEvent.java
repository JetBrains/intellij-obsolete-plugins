package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.IntermediateEventColorProvider;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.EventKind;
import com.intellij.jboss.bpmn.jbpm.render.label.RenderLabelColor;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tIntermediateThrowEvent interface.
 */
@RenderLabelColor(color = IntermediateEventColorProvider.class)
@EventKind("IntermediateThrow")
@DefaultNamePrefix("Intermediate Throw Event")
public interface TIntermediateThrowEvent extends Bpmn20DomElement, TThrowEvent {
}
