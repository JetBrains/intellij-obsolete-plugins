package com.intellij.jboss.bpmn.jbpm.layout;

import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.jboss.bpmn.jbpm.annotation.AnnotationCoordinator;
import com.intellij.jboss.bpmn.jbpm.model.ChartDataModel;
import com.intellij.jboss.bpmn.jbpm.model.ChartEdge;
import com.intellij.jboss.bpmn.jbpm.model.ChartNode;
import com.intellij.jboss.bpmn.jbpm.render.size.ChartNodeSizeEnhancer;
import com.intellij.jboss.bpmn.jbpm.render.size.RenderDefaultSize;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.DocumentReference;
import com.intellij.openapi.graph.base.Edge;
import com.intellij.openapi.graph.base.Node;
import com.intellij.openapi.graph.geom.YPoint;
import com.intellij.openapi.graph.layout.CopiedLayoutGraph;
import com.intellij.openapi.graph.layout.EdgeLayout;
import com.intellij.openapi.graph.layout.LayoutGraph;
import com.intellij.openapi.graph.layout.Layouter;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChartPersistentLayouter<T> implements Layouter {
  private static final int elementsSpace = 40;
  @NotNull private final ChartVirtualLayoutCoordinator chartVirtualLayoutCoordinator;
  @NotNull private final DiagramVfsResolver<T> vfsResolver;
  @NotNull private final AnnotationCoordinator<RenderDefaultSize> renderSizeCoordinator = new AnnotationCoordinator<>(
    RenderDefaultSize.class,
    RenderDefaultSize.class.getAnnotation(RenderDefaultSize.class));
  private final DiagramBuilder diagramBuilder;
  final private ChartDataModel<T> dataModel;
  //private final Graph2D graph;
  private final PsiFile[] psiFiles;

  public ChartPersistentLayouter(@NotNull ChartLayoutCoordinator coordinator,
                                 @NotNull DiagramVfsResolver<T> resolver,
                                 ChartDataModel<T> model,
                                 PsiFile[] psiFiles,
                                 DocumentReference[] references) {
    chartVirtualLayoutCoordinator = new ChartVirtualLayoutCoordinator(
      model.getProject(),
      references,
      false,
      coordinator);
    vfsResolver = resolver;
    dataModel = model;
    diagramBuilder = model.getBuilder();
    this.psiFiles = psiFiles;
  }

  @Override
  public boolean canLayout(LayoutGraph graph) {
    return true;
  }

  @Override
  public void doLayout(LayoutGraph layoutGraph) {
    List<ChartNode<T>> toLayout = new ArrayList<>();
    int maxY = 0;

    for (ChartNode<T> chartNode : dataModel.getNodes()) {
      ChartLayoutCoordinator.NodeLayout layout = layoutChartNode(chartNode, layoutGraph);
      if (layout != null && !isEmptyLayout(layout)) {
        maxY = (int)Math.max(maxY, layout.getBottom());
      }
      else {
        toLayout.add(chartNode);
      }
    }
    final int finalMaxY = maxY;
    if (toLayout.size() > 0) {
      WriteCommandAction.writeCommandAction(dataModel.getProject(), psiFiles).run(() -> {
        int x = elementsSpace;
        int y = finalMaxY + elementsSpace;

        for (ChartNode<T> node : toLayout) {

          x = layoutDiagramNodeVirtual(node, x, y, layoutGraph) + elementsSpace;
        }
      });
    }

    for (DiagramEdge<T> diagramEdge : dataModel.getEdges()) {
      layoutDiagramEdge(diagramEdge, layoutGraph);
    }
    diagramBuilder.getView().updateView();
  }

  private void layoutDiagramEdge(DiagramEdge<T> diagramEdge, LayoutGraph layoutGraph) {
    String fqnFrom = vfsResolver.getQualifiedName(diagramEdge.getSource().getIdentifyingElement());
    String fqnTo = vfsResolver.getQualifiedName(diagramEdge.getTarget().getIdentifyingElement());
    if (StringUtil.isEmpty(fqnFrom) || StringUtil.isEmpty(fqnTo)) {
      return;
    }
    List<ChartLayoutCoordinator.Point> points = chartVirtualLayoutCoordinator.getEdgePoints(fqnFrom, fqnTo);
    if (points == null || points.isEmpty()) {
      return;
    }
    layoutEdge(diagramEdge, layoutGraph, points);
  }

  private void layoutNode(ChartNode<T> chartNode, LayoutGraph layoutGraph, ChartLayoutCoordinator.NodeLayout nodeLayout) {
    Node node = diagramBuilder.getNode(chartNode);
    if (layoutGraph instanceof CopiedLayoutGraph) {
      Node copiedNode = ((CopiedLayoutGraph)layoutGraph).getCopiedNode(node);
      if (copiedNode != null) {
        layoutGraph.setCenter(copiedNode, nodeLayout.getCenterX(), nodeLayout.getCenterY());
      }
    }
    else {
      diagramBuilder.getGraph().setCenter(node, nodeLayout.getCenterX(), nodeLayout.getCenterY());
    }
  }

  private void layoutEdge(DiagramEdge<T> diagramEdge, LayoutGraph layoutGraph, List<ChartLayoutCoordinator.Point> points) {
    Edge edge = diagramBuilder.getEdge(diagramEdge);

    if (layoutGraph instanceof CopiedLayoutGraph) {
      Edge copiedEdge = ((CopiedLayoutGraph)layoutGraph).getCopiedEdge(edge);
      if (copiedEdge != null) {
        updateEdgePoints(points, layoutGraph.getLayout(copiedEdge));
      }
    }
    else {
      updateEdgePoints(points, diagramBuilder.getGraph().getLayout(edge));
    }
  }

  private void updateEdgePoints(List<ChartLayoutCoordinator.Point> points, EdgeLayout edgeLayout) {
    edgeLayout.clearPoints();
    for (ChartLayoutCoordinator.Point point : points) {
      edgeLayout.addPoint(point.x, point.y);
    }
  }

  private ChartLayoutCoordinator.NodeLayout layoutChartNode(ChartNode<T> chartNode, LayoutGraph layoutGraph) {
    String fqn = vfsResolver.getQualifiedName(chartNode.getIdentifyingElement());
    if (StringUtil.isEmpty(fqn)) {
      return null;
    }
    ChartLayoutCoordinator.NodeLayout nodeLayout = chartVirtualLayoutCoordinator
      .getNodeLayout(fqn, ChartNodeSizeEnhancer.enhancerForNode(chartNode));
    if (nodeLayout == null) {
      return null;
    }

    layoutNode(chartNode, layoutGraph, nodeLayout);

    return nodeLayout;
  }

  private int layoutDiagramNodeVirtual(ChartNode<T> chartNode, int x, int y, LayoutGraph layoutGraph) {
    String fqn = vfsResolver.getQualifiedName(chartNode.getIdentifyingElement());
    RenderDefaultSize size = renderSizeCoordinator.getAnnotation(chartNode.getClassesWithAnnotationsForRendering());
    ChartLayoutCoordinator.NodeLayout nodeLayout =
      ChartLayoutCoordinator.NodeLayout.createByEdges(x, y, x + size.width(), y + size.height());
    Runnable action =
      chartVirtualLayoutCoordinator.getCreateNodeLayoutAction(fqn, nodeLayout, ChartNodeSizeEnhancer.enhancerForNode(chartNode), true);
    if (action != null) {
      action.run();
    }
    layoutNode(chartNode, layoutGraph, nodeLayout);

    return (int)nodeLayout.getRight();
  }

  @Nullable
  public Runnable getDiagramNodeLayoutChangeAction(ChartNode<T> chartNode) {
    String fqn = vfsResolver.getQualifiedName(chartNode.getIdentifyingElement());
    if (StringUtil.isEmpty(fqn)) {
      return null;
    }
    Node node = diagramBuilder.getNode(chartNode);
    return chartVirtualLayoutCoordinator.getChangeNodeLayoutAction(
      fqn,
      ChartLayoutCoordinator.NodeLayout.createByCenterPoint(
        diagramBuilder.getGraph().getCenterX(node),
        diagramBuilder.getGraph().getCenterY(node),
        diagramBuilder.getGraph().getWidth(node),
        diagramBuilder.getGraph().getHeight(node)),
      ChartNodeSizeEnhancer.enhancerForNode(chartNode));
  }

  @Nullable
  private Runnable getDiagramEdgeLayoutChangeAction(ChartEdge<T> chartEdge) {
    String fqnSource = vfsResolver.getQualifiedName(chartEdge.getSource().getIdentifyingElement());
    String fqnTarget = vfsResolver.getQualifiedName(chartEdge.getTarget().getIdentifyingElement());
    if (StringUtil.isEmpty(fqnSource) || StringUtil.isEmpty(fqnTarget)) {
      return null;
    }
    Edge edge = diagramBuilder.getEdge(chartEdge);
    EdgeLayout edgeLayout = diagramBuilder.getGraph().getLayout(edge);
    List<ChartLayoutCoordinator.Point> points = new ArrayList<>();
    for (int i = 0; i < edgeLayout.pointCount(); ++i) {
      YPoint point = edgeLayout.getPoint(i);
      points.add(new ChartLayoutCoordinator.Point(point.getX(), point.getY()));
    }
    return chartVirtualLayoutCoordinator.getChangeEdgePointsAction(
      fqnSource,
      fqnTarget,
      points);
  }

  private static boolean isEmptyLayout(ChartLayoutCoordinator.NodeLayout layout) {
    return layout.getHeight() == 0 || layout.getWidth() == 0;
  }
}
