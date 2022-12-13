package com.intellij.seam.dependencies;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.graph.GraphManager;
import com.intellij.openapi.graph.builder.components.SelectionDependenciesPresentationModel;
import com.intellij.openapi.graph.builder.renderer.BasicGraphNodeRenderer;
import com.intellij.openapi.graph.layout.OrientationLayouter;
import com.intellij.openapi.graph.layout.hierarchic.HierarchicGroupLayouter;
import com.intellij.openapi.graph.layout.hierarchic.HierarchicLayouter;
import com.intellij.openapi.graph.services.GraphLayoutService;
import com.intellij.openapi.graph.services.GraphNodeRealizerService;
import com.intellij.openapi.graph.settings.GraphSettings;
import com.intellij.openapi.graph.view.*;
import com.intellij.openapi.module.Module;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.seam.dependencies.beans.SeamComponentNodeInfo;
import com.intellij.seam.dependencies.beans.SeamDependencyInfo;
import com.intellij.seam.dependencies.renderers.DefaultSeamComponentRenderer;
import com.intellij.util.OpenSourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class SeamDependenciesPresentationModel extends SelectionDependenciesPresentationModel<SeamComponentNodeInfo, SeamDependencyInfo> {
  private final Module myProject;
  private BasicGraphNodeRenderer myRenderer;

  public SeamDependenciesPresentationModel(final Graph2D graph, Module module) {
    super(graph);
    myProject = module;
    getSettings().setShowEdgeLabels(true);

    customizeDefaultSettings(getSettings());
  }

  private static void customizeDefaultSettings(final GraphSettings settings) {
    final HierarchicGroupLayouter groupLayouter = GraphLayoutService.getInstance().getGroupLayouter();

    groupLayouter.setOrientationLayouter(GraphManager.getGraphManager().createOrientationLayouter(OrientationLayouter.TOP_TO_BOTTOM));
    groupLayouter.setMinimalNodeDistance(20);
    groupLayouter.setMinimalLayerDistance(50);
    groupLayouter.setRoutingStyle(HierarchicLayouter.ROUTE_POLYLINE);
  }

  @Override
  @NotNull
  public NodeRealizer getNodeRealizer(final @Nullable SeamComponentNodeInfo node) {
    return GraphNodeRealizerService.getInstance().createGenericNodeRealizer("SeamComponentNodeInfoRenderer", getRenderer());
  }

  public BasicGraphNodeRenderer getRenderer() {
    if (myRenderer == null) {
      myRenderer = new DefaultSeamComponentRenderer(getGraphBuilder());
    }
    return myRenderer;
  }

  @Override
  @NotNull
  public EdgeRealizer getEdgeRealizer(final @Nullable SeamDependencyInfo edge) {
    final PolyLineEdgeRealizer edgeRealizer = GraphManager.getGraphManager().createPolyLineEdgeRealizer();

    edgeRealizer.setLineType(LineType.LINE_1);
    edgeRealizer.setLineColor(Color.GRAY);
    edgeRealizer.setArrow(Arrow.STANDARD);

    return edgeRealizer;
  }

  @Override
  public boolean editNode(final SeamComponentNodeInfo node) {
    final PsiElement psiElement = node.getIdentifyingElement().getIdentifyingPsiElement();
    if (psiElement instanceof Navigatable) {
      OpenSourceUtil.navigate((Navigatable)psiElement);
      return true;
    }
    return super.editNode(node);
  }

  @Override
  public boolean editEdge(final SeamDependencyInfo info) {
    final PsiElement psiElement = info.getIdentifyingElement().getIdentifyingPsiElement();
    if (psiElement instanceof Navigatable) {
      OpenSourceUtil.navigate((Navigatable)psiElement);
      return true;
    }
    return super.editEdge(info);
  }

  @Override
  public @Nullable String getNodeTooltip(final SeamComponentNodeInfo node) {
    return node.getName();
  }

  @Override
  public @Nullable String getEdgeTooltip(final SeamDependencyInfo edge) {
    return edge.getName();
  }

  @Override
  public void customizeSettings(final @NotNull Graph2DView view, final @NotNull EditMode editMode) {
    editMode.allowEdgeCreation(false);
    editMode.allowBendCreation(false);

    view.setFitContentOnResize(false);
    view.fitContent();
  }

  @Override
  public @Nullable NodeCellEditor getCustomNodeCellEditor(final @Nullable SeamComponentNodeInfo SeamComponentNodeInfo) {
    return null;
  }

  @Override
  public @NotNull DefaultActionGroup getNodeActionGroup(final @Nullable SeamComponentNodeInfo info) {
    return super.getNodeActionGroup(info);
  }

}
