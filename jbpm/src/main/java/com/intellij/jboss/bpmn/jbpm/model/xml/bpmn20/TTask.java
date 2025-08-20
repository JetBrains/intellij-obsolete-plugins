package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jboss.bpmn.jbpm.model.render.BpmnLabelProvider;
import com.intellij.jboss.bpmn.jbpm.render.label.RenderLabelPosition;
import com.intellij.jboss.bpmn.jbpm.render.label.RenderLabelText;
import com.intellij.jboss.bpmn.jbpm.render.size.RenderDefaultSize;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tTask interface.
 */
@Presentation(icon = "com.intellij.jboss.bpmn.JbossJbpmIcons.Bpmn.Task")
@RenderLabelText(text = BpmnLabelProvider.class)
@RenderLabelPosition(modelSpecifier = 1 /*NodeLabel.INTERNAL*/, positionSpecifier = 100 /*NodeLabel.CENTER*/)
@RenderDefaultSize(width = 120, height = 48)
public interface TTask extends Bpmn20DomElement, TActivity {
}
