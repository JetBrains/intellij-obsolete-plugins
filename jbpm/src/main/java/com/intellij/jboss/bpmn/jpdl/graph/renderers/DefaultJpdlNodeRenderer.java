package com.intellij.jboss.bpmn.jpdl.graph.renderers;

import com.intellij.jboss.bpmn.jpdl.graph.JpdlEdge;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNode;
import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.graph.builder.renderer.BasicGraphNodeRenderer;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;

public class DefaultJpdlNodeRenderer extends BasicGraphNodeRenderer<JpdlNode, JpdlEdge> {

  public DefaultJpdlNodeRenderer(GraphBuilder<JpdlNode, JpdlEdge> builder) {
    super(builder, ModificationTracker.EVER_CHANGED);
  }

  @Override
  protected Icon getIcon(JpdlNode node) {
    return node.getIcon();
  }

  @Override
  protected String getNodeName(JpdlNode node) {
    return node.getName();
  }

  @Override
  protected Color getBackground(JpdlNode node) {
    return JBColor.LIGHT_GRAY.brighter();
  }
}
