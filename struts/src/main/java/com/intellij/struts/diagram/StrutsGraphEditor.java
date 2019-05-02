/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.struts.diagram;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.graph.GraphManager;
import com.intellij.openapi.graph.base.Node;
import com.intellij.openapi.graph.base.NodeCursor;
import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.graph.builder.GraphBuilderFactory;
import com.intellij.openapi.graph.builder.components.BasicGraphComponent;
import com.intellij.openapi.graph.builder.util.GraphViewUtil;
import com.intellij.openapi.graph.view.Graph2D;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.struts.dom.StrutsConfig;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomEventListener;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.events.DomEvent;
import com.intellij.util.xml.ui.PerspectiveFileEditor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public class StrutsGraphEditor extends PerspectiveFileEditor {

  private BasicGraphComponent<StrutsObject, StrutsObject> myComponent;
  private final StrutsConfig myStrutsConfig;

  protected StrutsGraphEditor(final Project project, final VirtualFile file, final StrutsConfig strutsConfig) {
    super(project, file);

    myStrutsConfig = strutsConfig;
  }

  private void initialize(final Project project) {
    final Graph2D graph = GraphManager.getGraphManager().createGraph2D();
    final StrutsGraphDataModel dataModel = new StrutsGraphDataModel(myStrutsConfig);

    final Graph2DView view = GraphManager.getGraphManager().createGraph2DView();
    final StrutsPresentationModel presentationModel = new StrutsPresentationModel(project,
                                                                                  myStrutsConfig.getManager(),
                                                                                  dataModel,
                                                                                  graph);
    presentationModel.setShowEdgeLabels(true);

    final GraphBuilder<StrutsObject, StrutsObject> graphBuilder =
      GraphBuilderFactory.getInstance(project).createGraphBuilder(graph, view, dataModel, presentationModel);

    myComponent = new BasicGraphComponent<>(graphBuilder);

    final DataProvider dataProvider = dataId -> {
      if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
        final NodeCursor nodeCursor = graph.selectedNodes();
        if (nodeCursor != null && nodeCursor.size() > 0) {
          final Node node = nodeCursor.node();
          if (node != null) {
            final StrutsObject nodeObject = graphBuilder.getNodeObject(node);
            if (nodeObject != null) {
              final PsiElement psiElement = nodeObject.getPsiElement();
              if (psiElement instanceof Navigatable) {
                return psiElement;
              }
            }
          }
        }
      }
      else if (PlatformDataKeys.HELP_ID.is(dataId)) {
        return "reference.struts.webflow";
      }
      return null;
    };
    GraphViewUtil.addDataProvider(view, dataProvider);

    myStrutsConfig.getManager().addDomEventListener(event -> {
      dataModel.setDirty();
      graphBuilder.queueUpdate();
    }, this);
    DomManager.getDomManager(project).addDomEventListener(new DomEventListener() {
      @Override
      public void eventOccured(@NotNull DomEvent event) {
        if (getComponent().isShowing()) {
          graphBuilder.updateGraph();
        }
      }
    }, this);


    Disposer.register(this, myComponent);
  }

  @Override
  protected DomElement getSelectedDomElement() {
    return null;
  }

  @Override
  protected void setSelectedDomElement(DomElement domElement) {

  }

  @Override
  @NotNull
  protected JComponent createCustomComponent() {
    if (myComponent == null) {
      initialize(getProject());
    }
    return myComponent.getComponent();
  }

  @Override
  @Nullable
  public JComponent getPreferredFocusedComponent() {
    return myComponent.getBuilder().getView().getCanvasComponent();
  }

  @Override
  @NonNls
  @NotNull
  public String getName() {
    return "Web Flow Diagram";
  }

  @Override
  public void commit() {

  }

  @Override
  public void reset() {
    myComponent.getBuilder().updateGraph();
  }
}
