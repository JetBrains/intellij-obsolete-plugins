package com.intellij.jboss.bpmn.jbpm.chart;

import com.intellij.jboss.bpmn.jbpm.BpmnUtils;
import com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartDataModel;
import com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartEdge;
import com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartNode;
import com.intellij.jboss.bpmn.jbpm.layout.ChartLayoutCoordinator;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc.Bounds;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi.BPMNEdge;
import com.intellij.jboss.bpmn.jbpm.render.size.ChartNodeSizeEnhancer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BpmnChartLayoutCoordinator implements ChartLayoutCoordinator {
  @NotNull private final Project project;
  @NotNull private final BpmnChartDataModel dataModel;

  public BpmnChartLayoutCoordinator(@NotNull Project project, @NotNull BpmnChartDataModel dataModel) {
    this.project = project;
    this.dataModel = dataModel;
  }

  @Nullable
  @Override
  public List<Point> getEdgePoints(@NotNull String sourceNodeId, @NotNull String targetNodeId) {
    BpmnChartEdge chartEdge = dataModel.getEdge(Pair.create(sourceNodeId, targetNodeId));
    if (chartEdge == null || chartEdge.getLayout() == null) {
      return null;
    }
    return JBIterable.from(chartEdge.getLayout().getWaypoints())
      .transform(point -> new Point(
        BpmnUtils.getAttributeValue(point.getX()),
        BpmnUtils.getAttributeValue(point.getY())))
      .toList();
  }

  @Override
  @Nullable
  public Runnable getChangeEdgePointsAction(@NotNull String sourceNodeId, @NotNull String targetNodeId, @NotNull final List<Point> points) {
    BpmnChartEdge chartEdge = dataModel.getEdge(Pair.create(sourceNodeId, targetNodeId));
    if (chartEdge == null) {
      return null;
    }
    final BPMNEdge edgeLayout = chartEdge.getLayout();
    if (edgeLayout == null) {
      return null;
    }
    if (points.equals(getEdgePoints(sourceNodeId, targetNodeId))) {
      return null;
    }
    return () -> {
      List<com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc.Point> wayPoints = edgeLayout.getWaypoints();
      for (com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc.Point point : wayPoints) {
        point.undefine();
      }
      for (Point point : points) {
        com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc.Point wayPoint = edgeLayout.addWaypoint();
        wayPoint.getX().setValue(point.x);
        wayPoint.getY().setValue(point.y);
      }
    };
  }

  @Nullable
  @Override
  public NodeLayout getNodeLayout(@NotNull String fqn, ChartNodeSizeEnhancer enhancer) {
    BpmnChartNode chartNode = dataModel.getNode(fqn);
    if (chartNode == null || chartNode.getLayout() == null) {
      return null;
    }
    Bounds bounds = chartNode.getLayout().getBounds();

    return createNodeLayout(bounds, enhancer);
  }

  @Override
  @Nullable
  public Runnable getChangeNodeLayoutAction(@NotNull final String fqn,
                                            @Nullable final NodeLayout layout,
                                            @Nullable ChartNodeSizeEnhancer enhancer) {
    final BpmnChartNode chartNode = dataModel.getNode(fqn);
    if (chartNode == null) {
      return null;
    }
    if (chartNode.getLayout() == null) {
      if (layout == null) {
        return null;
      }
      return () -> chartNode.setLayout(dataModel.createNodeShape(fqn, layout.getCenter(), layout.getSize()));
    }
    if (layout == null) {
      return () -> dataModel.getNode(fqn).removeLayout();
    }

    Bounds bounds = chartNode.getLayout().getBounds();

    NodeLayout oldLayout = createNodeLayout(bounds, enhancer);
    if (oldLayout.equals(layout)) {
      return null;
    }

    Size oldSize = createNodeSize(bounds, enhancer);
    Size newSize = new Size(layout.getWidth(), layout.getHeight());
    final boolean resizeNode = !oldSize.equals(newSize);

    return () -> {
      Bounds bounds1 = chartNode.getLayout().getBounds();
      bounds1.getX().setValue(layout.getLeft());
      bounds1.getY().setValue(layout.getTop());
      if (resizeNode) {
        bounds1.getWidth().setValue(layout.getWidth());
        bounds1.getHeight().setValue(layout.getHeight());
      }
    };
  }

  @NotNull
  private static Size createNodeSize(Bounds bounds, @Nullable ChartNodeSizeEnhancer enhancer) {
    Size size = new Size(BpmnUtils.getAttributeValue(bounds.getWidth()),
                         BpmnUtils.getAttributeValue(bounds.getHeight()));
    if (enhancer != null) {
      size = enhancer.enhance(size);
    }
    return size;
  }

  @NotNull
  private static NodeLayout createNodeLayout(Bounds bounds, @Nullable ChartNodeSizeEnhancer enhancer) {
    Size size = createNodeSize(bounds, enhancer);
    double x = BpmnUtils.getAttributeValue(bounds.getX());
    double y = BpmnUtils.getAttributeValue(bounds.getY());
    return NodeLayout.createByEdges(
      x,
      y,
      x + size.width,
      y + size.height);
  }
}
