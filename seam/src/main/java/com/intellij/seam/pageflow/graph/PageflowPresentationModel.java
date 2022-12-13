package com.intellij.seam.pageflow.graph;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.Constraints;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.graph.GraphManager;
import com.intellij.openapi.graph.builder.DeleteProvider;
import com.intellij.openapi.graph.builder.SimpleNodeCellEditor;
import com.intellij.openapi.graph.builder.components.BasicGraphPresentationModel;
import com.intellij.openapi.graph.builder.renderer.BasicGraphNodeRenderer;
import com.intellij.openapi.graph.layout.OrientationLayouter;
import com.intellij.openapi.graph.layout.hierarchic.HierarchicGroupLayouter;
import com.intellij.openapi.graph.layout.hierarchic.HierarchicLayouter;
import com.intellij.openapi.graph.services.GraphLayoutService;
import com.intellij.openapi.graph.services.GraphNodeRealizerService;
import com.intellij.openapi.graph.settings.GraphSettings;
import com.intellij.openapi.graph.view.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.pom.Navigatable;
import com.intellij.psi.xml.XmlElement;
import com.intellij.seam.pageflow.graph.renderers.DefaultPageflowNodeRenderer;
import com.intellij.seam.pageflow.model.xml.pageflow.PageflowNamedElement;
import com.intellij.util.OpenSourceUtil;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class PageflowPresentationModel extends BasicGraphPresentationModel<PageflowNode, PageflowEdge> {
  private final Project myProject;
  private BasicGraphNodeRenderer myRenderer;

  public PageflowPresentationModel(final Graph2D graph, Project project) {
    super(graph);
    myProject = project;
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
  public NodeRealizer getNodeRealizer(final @Nullable PageflowNode node) {
    return GraphNodeRealizerService.getInstance().createGenericNodeRealizer("PageflowNodeRenderer", getRenderer());
  }

  public BasicGraphNodeRenderer getRenderer() {
    if (myRenderer == null) {
      myRenderer = new DefaultPageflowNodeRenderer(getGraphBuilder());
    }
    return myRenderer;
  }

  @Override
  @NotNull
  public EdgeRealizer getEdgeRealizer(final @Nullable PageflowEdge edge) {
    final PolyLineEdgeRealizer edgeRealizer = GraphManager.getGraphManager().createPolyLineEdgeRealizer();

    edgeRealizer.setLineType(LineType.LINE_1);
    edgeRealizer.setLineColor(Color.GRAY);
    edgeRealizer.setArrow(Arrow.STANDARD);

    return edgeRealizer;
  }

  @Override
  public boolean editNode(final PageflowNode node) {
    return super.editNode(node);
  }

  @Override
  public boolean editEdge(final PageflowEdge pageflowEdge) {
    // todo inplace editing
    // final Edge edge = getGraphBuilder().getEdge(pageflowEdge);
    //final EdgeRealizer realizer = getGraphBuilder().getGraph().getRealizer(edge);
    //
    //final EdgeLabel edgeLabel = realizer.getLabel();
    //if (edgeLabel != null) {
    //  final PropertyChangeListener listener = new PropertyChangeListener() {
    //    public void propertyChange(final PropertyChangeEvent evt) {
    //      final String newValue = evt.getNewValue().toString();
    //      if (newValue != null) {
    //        new WriteCommandAction(getProject()) {
    //          protected void run(final Result result) throws Throwable {
    //            pageflowEdge.getIdentifyingElement().getName().setStringValue(newValue);
    //          }
    //        }.execute();
    //      }
    //    }
    //  };
    //
    //  getGraphBuilder().getView().openLabelEditor(edgeLabel, edgeLabel.getBox().getX(), edgeLabel.getBox().getY(), listener, true);
    //}
    final XmlElement xmlElement = pageflowEdge.getIdentifyingElement().getXmlElement();
    if (xmlElement instanceof Navigatable) {
      OpenSourceUtil.navigate((Navigatable)xmlElement);
      return true;
    }
    return super.editEdge(pageflowEdge);
  }

  public Project getProject() {
    return myProject;
  }


  @Override
  public @Nullable String getNodeTooltip(final PageflowNode node) {
    return node.getName();
  }

  @Override
  public @Nullable String getEdgeTooltip(final PageflowEdge edge) {
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
    return new DeleteProvider<PageflowNode, PageflowEdge>() {
      @Override
      public boolean canDeleteNode(@NotNull final PageflowNode node) {
        return !getGraphBuilder().isCellEditing();
      }

      @Override
      public boolean canDeleteEdge(@NotNull final PageflowEdge edge) {
        return true;
      }

      @Override
      public boolean deleteNode(@NotNull final PageflowNode node) {
        WriteCommandAction.writeCommandAction(getProject()).run(() -> node.getIdentifyingElement().undefine());

        return true;
      }

      @Override
      public boolean deleteEdge(@NotNull final PageflowEdge edge) {
        WriteCommandAction.writeCommandAction(getProject()).run(() -> edge.getIdentifyingElement().undefine());

        return true;
      }
    };
  }

  @Override
  public @Nullable NodeCellEditor getCustomNodeCellEditor(final @Nullable PageflowNode pageflowNode) {
    return new SimpleNodeCellEditor<>(pageflowNode, getProject()) {
      @Override
      protected String getEditorValue(final PageflowNode value) {
        final String s = value.getName();
        return s == null ? "" : s;
      }

      @Override
      protected void setEditorValue(final PageflowNode value, final String newValue) {
        final DomElement element = value.getIdentifyingElement();
        if (element instanceof PageflowNamedElement) {
          WriteCommandAction.writeCommandAction(myProject).run(() -> ((PageflowNamedElement)element).getName().setStringValue(newValue));
        }

        IdeFocusManager.getInstance(getProject()).requestFocus(getGraphBuilder().getView().getJComponent(), true);
      }
    };
  }

  @Override
  public @NotNull DefaultActionGroup getNodeActionGroup(final @Nullable PageflowNode pageflowNode) {
    final DefaultActionGroup group = super.getNodeActionGroup(pageflowNode);

    group.add(ActionManager.getInstance().getAction("Pageflow.Designer"), Constraints.FIRST);

    return group;
  }
}
