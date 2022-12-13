package com.intellij.seam.pages.graph;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.graph.GraphManager;
import com.intellij.openapi.graph.builder.DeleteProvider;
import com.intellij.openapi.graph.builder.EdgeCreationPolicy;
import com.intellij.openapi.graph.builder.components.SelectionDependenciesPresentationModel;
import com.intellij.openapi.graph.builder.renderer.BasicGraphNodeRenderer;
import com.intellij.openapi.graph.services.GraphNodeRealizerService;
import com.intellij.openapi.graph.view.*;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.xml.XmlElement;
import com.intellij.seam.pages.graph.renderers.DefaultPagesNodeRenderer;
import com.intellij.seam.pages.graph.beans.*;
import com.intellij.util.OpenSourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PagesPresentationModel extends SelectionDependenciesPresentationModel<BasicPagesNode, BasicPagesEdge> {
  private final Project myProject;
  private BasicGraphNodeRenderer myRenderer;

  public PagesPresentationModel(final Graph2D graph, Project project) {
    super(graph);
    myProject = project;
    getSettings().setShowEdgeLabels(false);
  }

  @Override
  @NotNull
  public NodeRealizer getNodeRealizer(final @Nullable BasicPagesNode node) {
    return GraphNodeRealizerService.getInstance().createGenericNodeRealizer("PagesNodeRenderer", getRenderer());
  }

  public BasicGraphNodeRenderer getRenderer() {
    if (myRenderer == null) {
      myRenderer = new DefaultPagesNodeRenderer(getGraphBuilder());
    }
    return myRenderer;
  }

  @Override
  @NotNull
  public EdgeRealizer getEdgeRealizer(final @Nullable BasicPagesEdge edge) {
    final PolyLineEdgeRealizer edgeRealizer = GraphManager.getGraphManager().createPolyLineEdgeRealizer();

    final boolean isExceptionEdge = edge instanceof ExceptionEdge;
    edgeRealizer.setLineType(isExceptionEdge ? LineType.DASHED_1 : LineType.LINE_1);
    edgeRealizer.setArrow(isExceptionEdge? Arrow.DELTA : Arrow.STANDARD);
    edgeRealizer.setLineColor(Color.GRAY);

    return edgeRealizer;
  }

  @Override
  public boolean editNode(final BasicPagesNode node) {
    final XmlElement xmlElement = node.getIdentifyingElement().getXmlElement();
    if (xmlElement instanceof Navigatable) {
      OpenSourceUtil.navigate((Navigatable)xmlElement);
      return true;
    }
    return super.editNode(node);
  }

  @Override
  public boolean editEdge(final BasicPagesEdge pagesEdge) {
    final XmlElement xmlElement = pagesEdge.getViewId().getXmlElement();
    if (xmlElement instanceof Navigatable) {
      OpenSourceUtil.navigate((Navigatable)xmlElement);
      return true;
    }
    return super.editEdge(pagesEdge);
  }

  public Project getProject() {
    return myProject;
  }


  @Override
  public @Nullable String getNodeTooltip(final BasicPagesNode node) {
    return node.getName();
  }

  @Override
  public @Nullable String getEdgeTooltip(final BasicPagesEdge edge) {
    return edge.getName();
  }

  @Override
  public void customizeSettings(final @NotNull Graph2DView view, final @NotNull EditMode editMode) {
    editMode.allowEdgeCreation(true);
    editMode.allowBendCreation(false);

    view.setFitContentOnResize(false);
    view.fitContent();
  }

  @Override
  public @Nullable DeleteProvider getDeleteProvider() {
    return new DeleteProvider<BasicPagesNode, BasicPagesEdge>() {
      @Override
      public boolean canDeleteNode(@NotNull final BasicPagesNode node) {
        return !getGraphBuilder().isCellEditing();
      }

      @Override
      public boolean canDeleteEdge(@NotNull final BasicPagesEdge edge) {
        return true;
      }

      @Override
      public boolean deleteNode(@NotNull final BasicPagesNode node) {
        final Collection<BasicPagesEdge> edges = getGraphBuilder().getEdgeObjects();

        final List<BasicPagesEdge> deleteThis = new ArrayList<>();
        for (BasicPagesEdge edge : edges) {
          if (edge.getSource().equals(node) || edge.getTarget().equals(node)) {
            deleteThis.add(edge);
          }
        }

        WriteCommandAction.writeCommandAction(getProject()).run(() -> {
          node.getIdentifyingElement().undefine();

          for (BasicPagesEdge edge : deleteThis) {
            if (edge.getViewId().isValid()) {
              edge.getViewId().undefine();
            }
          }
        });


        return true;
      }

      @Override
      public boolean deleteEdge(@NotNull final BasicPagesEdge edge) {
        WriteCommandAction.writeCommandAction(getProject()).run(() -> edge.getViewId().undefine());

        return true;
      }
    };
  }

  @Override
  public @Nullable NodeCellEditor getCustomNodeCellEditor(final @Nullable BasicPagesNode pagesNode) {
    return null;
  }

  @Override
  public @NotNull DefaultActionGroup getNodeActionGroup(final @Nullable BasicPagesNode pagesNode) {
    return super.getNodeActionGroup(pagesNode);
  }

  @Override
  public @NotNull EdgeCreationPolicy<BasicPagesNode> getEdgeCreationPolicy() {
    return new EdgeCreationPolicy<>() {
      @Override
      public boolean acceptSource(@NotNull final BasicPagesNode source) {
        return source instanceof PageNode || source instanceof ExceptionNode;
      }

      @Override
      public boolean acceptTarget(@NotNull final BasicPagesNode target) {
        return !target.getName().contains("*") && !(target instanceof ExceptionNode);
      }
    };
  }
}
