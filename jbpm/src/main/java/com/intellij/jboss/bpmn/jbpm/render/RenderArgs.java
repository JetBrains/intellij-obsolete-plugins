package com.intellij.jboss.bpmn.jbpm.render;

import com.intellij.diagram.DiagramBuilder;
import com.intellij.jboss.bpmn.jbpm.model.ChartNode;

import javax.swing.*;
import java.awt.*;

public class RenderArgs<T> {
  public final ChartNode<T> chartNode;
  public final DiagramBuilder builder;
  public final Point point;
  public final JPanel wrapper;
  public final ChartNodeMainPanel panel;

  public RenderArgs(ChartNode<T> chartNode, DiagramBuilder builder, Point point, JPanel wrapper, ChartNodeMainPanel panel) {
    this.chartNode = chartNode;
    this.builder = builder;
    this.point = point;
    this.wrapper = wrapper;
    this.panel = panel;
  }
}
