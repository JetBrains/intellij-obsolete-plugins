package com.intellij.jboss.bpmn.jpdl.graph;

import com.intellij.jboss.bpmn.jpdl.graph.renderers.DefaultJpdlNodeRenderer;
import com.intellij.jboss.bpmn.jpdl.model.xml.JpdlNamedActivity;
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
import com.intellij.ui.JBColor;
import com.intellij.util.OpenSourceUtil;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JpdlPresentationModel extends BasicGraphPresentationModel<JpdlNode, JpdlEdge> {
  private final Project myProject;
  private BasicGraphNodeRenderer myRenderer;

  public JpdlPresentationModel(final Graph2D graph, Project project) {
    super(graph);
    myProject = project;
    getSettings().setShowEdgeLabels(true);

    customizeDefaultSettings(getSettings());
  }

  @Override
  @NotNull
  public NodeRealizer getNodeRealizer(final @Nullable JpdlNode node) {
    return GraphNodeRealizerService.getInstance().createGenericNodeRealizer("JpdlNodeRenderer", getRenderer());
  }

  public BasicGraphNodeRenderer getRenderer() {
    if (myRenderer == null) {
      myRenderer = new DefaultJpdlNodeRenderer(getGraphBuilder());
    }
    return myRenderer;
  }

  @Override
  @NotNull
  public EdgeRealizer getEdgeRealizer(final com.intellij.jboss.bpmn.jpdl.graph.@Nullable JpdlEdge e) {

    return super.getEdgeRealizer(e);
  }

  @Override
  public boolean editNode(final @Nullable JpdlNode node) {
    return super.editNode(node);
  }

  @Override
  public boolean editEdge(final JpdlEdge jpdlEdge) {
    final XmlElement xmlElement = jpdlEdge.getIdentifyingElement().getXmlElement();
    if (xmlElement instanceof Navigatable) {
      OpenSourceUtil.navigate(true, (Navigatable)xmlElement);
      return true;
    }
    return super.editEdge(jpdlEdge);
  }

  public Project getProject() {
    return myProject;
  }

  @Override
  public @Nullable String getNodeTooltip(final JpdlNode node) {
    final XmlElement xmlElement = node.getIdentifyingElement().getXmlElement();
    if (xmlElement != null) {
      return xmlElement.getText();
    }
    return node.getName();
  }

  @Override
  public @Nullable String getEdgeTooltip(final JpdlEdge edge) {
    final XmlElement xmlElement = edge.getIdentifyingElement().getXmlElement();
    if (xmlElement != null) {
      return xmlElement.getText();
    }
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
    return new DeleteProvider<JpdlNode, JpdlEdge>() {
      @Override
      public boolean canDeleteNode(@NotNull final JpdlNode node) {
        return !getGraphBuilder().isCellEditing();
      }

      @Override
      public boolean canDeleteEdge(@NotNull final JpdlEdge edge) {
        return true;
      }

      @Override
      public boolean deleteNode(@NotNull final JpdlNode node) {
        WriteCommandAction.writeCommandAction(getProject()).run(() -> node.getIdentifyingElement().undefine());

        return true;
      }

      @Override
      public boolean deleteEdge(@NotNull final JpdlEdge edge) {
        WriteCommandAction.writeCommandAction(getProject()).run(() -> edge.getIdentifyingElement().undefine());

        return true;
      }
    };
  }

  @Override
  public @Nullable NodeCellEditor getCustomNodeCellEditor(final @Nullable JpdlNode jpdlNode) {
    return new SimpleNodeCellEditor<>(jpdlNode, getProject()) {
      @Override
      protected String getEditorValue(final JpdlNode value) {
        final String s = value.getName();
        return s == null ? "" : s;
      }

      @Override
      protected void setEditorValue(final JpdlNode value, final String newValue) {
        final DomElement element = value.getIdentifyingElement();
        if (element instanceof JpdlNamedActivity) {
          WriteCommandAction.writeCommandAction(myProject).run(() -> ((JpdlNamedActivity)element).getName().setStringValue(newValue));
        }

        IdeFocusManager.getInstance(getProject()).requestFocus(getGraphBuilder().getView().getJComponent(), true);
      }
    };
  }

  @Override
  public @NotNull DefaultActionGroup getNodeActionGroup(final @Nullable JpdlNode jpdlNode) {
    final DefaultActionGroup group = super.getNodeActionGroup(jpdlNode);

    group.add(ActionManager.getInstance().getAction("Jpdl.Designer"), Constraints.FIRST);

    return group;
  }

  private static void customizeDefaultSettings(final GraphSettings settings) {
    final HierarchicGroupLayouter groupLayouter = GraphLayoutService.getInstance().getGroupLayouter();

    groupLayouter.setOrientationLayouter(GraphManager.getGraphManager().createOrientationLayouter(OrientationLayouter.TOP_TO_BOTTOM));
    groupLayouter.setMinimalNodeDistance(20);
    groupLayouter.setMinimalLayerDistance(50);
    groupLayouter.setRoutingStyle(HierarchicLayouter.ROUTE_POLYLINE);
  }
}
