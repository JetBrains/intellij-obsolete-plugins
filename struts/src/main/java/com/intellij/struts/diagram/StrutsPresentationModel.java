/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.diagram;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.graph.base.Graph;
import com.intellij.openapi.graph.builder.EdgeCreationPolicy;
import com.intellij.openapi.graph.builder.components.BasicGraphPresentationModel;
import com.intellij.openapi.graph.builder.util.GraphViewUtil;
import com.intellij.openapi.graph.view.EditMode;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.graph.view.NodeRealizer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.pom.Navigatable;
import com.intellij.util.OpenSourceUtil;
import org.jetbrains.annotations.NotNull;

public class StrutsPresentationModel extends BasicGraphPresentationModel<StrutsObject, StrutsObject> {

  private StrutsGraphNodeRenderer myNodeCellRenderer;
  private final Project myProject;
  private final ModificationTracker myTracker;

  public StrutsPresentationModel(final Project project,
                                 final ModificationTracker tracker,
                                 final StrutsGraphDataModel dataModel,
                                 final Graph graph) {
    super(graph);
    myProject = project;
    myTracker = tracker;
  }

  @Override
  public void customizeSettings(final Graph2DView view, final EditMode editMode) {
    editMode.allowEdgeCreation(false);
  }

  @Override
  @NotNull
  public NodeRealizer getNodeRealizer(final StrutsObject strutsObject) {
    return GraphViewUtil.createNodeRealizer("Struts", getNodeCellRenderer());
  }

  @Override
  public boolean editNode(final StrutsObject strutsObject) {
    if (strutsObject.getPsiElement() instanceof Navigatable) {
      OpenSourceUtil.navigate((Navigatable)strutsObject.getPsiElement());
      return true;
    }
    return super.editNode(strutsObject);
  }

  @Override
  public boolean editEdge(final StrutsObject strutsObject) {
    if (strutsObject.getPsiElement() instanceof Navigatable) {
      OpenSourceUtil.navigate((Navigatable)strutsObject.getPsiElement());
      return true;
    }
    return super.editNode(strutsObject);
  }

  @Override
  protected DefaultActionGroup getCommonActionGroup() {
    final DefaultActionGroup group = super.getCommonActionGroup();
    final AnAction anAction = ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE);
    group.add(Separator.getInstance(), Constraints.FIRST);
    group.add(anAction, Constraints.FIRST);
    return group;
  }


  @Override
  public EdgeCreationPolicy<StrutsObject> getEdgeCreationPolicy() {
    return super.getEdgeCreationPolicy();
  }

  public Project getProject() {
    return myProject;
  }

  public ModificationTracker getTracker() {
    return myTracker;
  }

  public StrutsGraphNodeRenderer getNodeCellRenderer() {
    if (myNodeCellRenderer == null) {
      myNodeCellRenderer = new StrutsGraphNodeRenderer(getGraphBuilder(), getProject(), getTracker());
    }
    return myNodeCellRenderer;
  }
}
