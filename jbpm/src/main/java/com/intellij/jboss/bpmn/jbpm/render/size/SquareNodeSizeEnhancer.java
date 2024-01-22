package com.intellij.jboss.bpmn.jbpm.render.size;

import com.intellij.jboss.bpmn.jbpm.layout.ChartLayoutCoordinator;

public class SquareNodeSizeEnhancer extends ChartNodeSizeEnhancer {
  @Override
  public ChartLayoutCoordinator.Size enhance(ChartLayoutCoordinator.Size size) {
    double min = Math.min(size.width, size.height);
    return Double.compare(size.width, size.height) == 0 ? size : new ChartLayoutCoordinator.Size(min, min);
  }
}
