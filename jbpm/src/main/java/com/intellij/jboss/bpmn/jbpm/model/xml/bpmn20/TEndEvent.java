package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jboss.bpmn.jbpm.model.render.EndEventColorProvider;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.EventKind;
import com.intellij.jboss.bpmn.jbpm.render.label.RenderLabelColor;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tEndEvent interface.
 */
@Presentation(icon = "com.intellij.jboss.bpmn.JbossJbpmIcons.Bpmn.End")
@RenderLabelColor(color = EndEventColorProvider.class)
@EventKind("End")
@DefaultNamePrefix("End Event")
public interface TEndEvent extends Bpmn20DomElement, TThrowEvent {

}
