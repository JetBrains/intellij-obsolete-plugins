package com.intellij.jboss.bpmn.jbpm.model.render;

import com.intellij.jboss.bpmn.jbpm.providers.TransparentColorProvider;
import com.intellij.jboss.bpmn.jbpm.render.background.RenderBackgroundColor;
import com.intellij.jboss.bpmn.jbpm.render.background.RenderBorder;
import com.intellij.jboss.bpmn.jbpm.render.label.RenderLabelPosition;
import com.intellij.jboss.bpmn.jbpm.render.label.RenderLabelText;
import com.intellij.jboss.bpmn.jbpm.render.size.RenderDefaultSize;
import com.intellij.jboss.bpmn.jbpm.render.size.SizeEnhancer;
import com.intellij.jboss.bpmn.jbpm.render.size.SquareNodeSizeEnhancer;

@RenderBorder(width = 0)
@RenderBackgroundColor(color = TransparentColorProvider.class)
@RenderLabelText(text = BpmnLabelProvider.class)
@RenderLabelPosition(modelSpecifier = 3 /*NodeLabel.SIDES*/, positionSpecifier = 109 /*NodeLabel.S*/)
@RenderDefaultSize(width = 48, height = 48)
@SizeEnhancer(SquareNodeSizeEnhancer.class)
public interface SquareImage48x48 {
}
