package com.intellij.jboss.bpmn.jpdl.fileEditor;

import com.intellij.jboss.bpmn.jpdl.graph.JpdlDataModel;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlEdge;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNode;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlPresentationModel;
import com.intellij.jboss.bpmn.jpdl.graph.dnd.JpdlDnDSupport;
import com.intellij.jboss.bpmn.jpdl.model.xml.JpdlNamedActivity;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.graph.GraphDataKeys;
import com.intellij.openapi.graph.GraphManager;
import com.intellij.openapi.graph.base.Node;
import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.graph.builder.GraphBuilderFactory;
import com.intellij.openapi.graph.builder.actions.AbstractGraphAction;
import com.intellij.openapi.graph.builder.dnd.GraphDnDUtils;
import com.intellij.openapi.graph.builder.dnd.SimpleDnDPanel;
import com.intellij.openapi.graph.geom.YRectangle;
import com.intellij.openapi.graph.services.GraphSelectionService;
import com.intellij.openapi.graph.view.Graph2D;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.graph.view.Overview;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomElementNavigationProvider;
import com.intellij.util.xml.DomEventListener;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.events.DomEvent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JpdlDesignerComponent extends JPanel implements DataProvider, Disposable {
  @NonNls
  private static final String JPDL_DESIGNER_NAVIGATION_PROVIDER_NAME = "JPDL_DESIGNER_NAVIGATION_PROVIDER_NAME";

  @NonNls
  private final SeamProcessDesignerNavigationProvider myNavigationProvider = new SeamProcessDesignerNavigationProvider();

  private final GraphBuilder<JpdlNode, JpdlEdge> myBuilder;
  private final XmlFile myXmlFile;
  private final JpdlDataModel myDataModel;

  public JpdlDesignerComponent(final XmlFile xmlFile) {
    myXmlFile = xmlFile;
    final Project project = xmlFile.getProject();

    final Graph2D graph = GraphManager.getGraphManager().createGraph2D();
    final Graph2DView view = GraphManager.getGraphManager().createGraph2DView();
    myDataModel = new JpdlDataModel(xmlFile);
    JpdlPresentationModel presentationModel = new JpdlPresentationModel(graph, project);

    myBuilder = GraphBuilderFactory.getInstance(project).createGraphBuilder(graph, view, myDataModel, presentationModel);
    Disposer.register(this, myBuilder);

    GraphDataKeys.addDataProvider(view, new MyDataProvider(myBuilder));

    final Splitter splitter = new Splitter(false, 0.85f);

    setLayout(new BorderLayout());

    final SimpleDnDPanel simpleDnDPanel = GraphDnDUtils.createDnDActions(project, myBuilder, new JpdlDnDSupport(myDataModel));
    final JComponent graphComponent = myBuilder.getView().getJComponent();

    splitter.setSecondComponent(simpleDnDPanel.getTree());

    splitter.setFirstComponent(graphComponent);
    splitter.setDividerWidth(5);

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
      ActionPlaces.TOOLBAR, AbstractGraphAction.getCommonToolbarActions(), true);
    toolbar.setTargetComponent(graphComponent);

    add(toolbar.getComponent(), BorderLayout.NORTH);
    add(splitter, BorderLayout.CENTER);

    myBuilder.initialize();

    DomManager.getDomManager(myBuilder.getProject()).addDomEventListener(new DomEventListener() {
      @Override
      public void eventOccured(@NotNull final DomEvent event) {
        if (isShowing()) {
          simpleDnDPanel.updateTree();
          myBuilder.queueUpdate();
        }
      }
    }, this);
  }

  public List<DomElement> getSelectedDomElements() {
    final var selected = new ArrayList<DomElement>();
    GraphSelectionService.getInstance().forEachSelectedNode(myBuilder.getGraph(), node -> {
      final var nodeObject = myBuilder.getNodeObject(node);
      if (nodeObject != null) {
        ContainerUtil.addIfNotNull(selected, nodeObject.getIdentifyingElement());
      }
    });
    return selected;
  }

  public void setSelectedDomElement(final DomElement domElement) {
    if (domElement == null) return;

    final JpdlNamedActivity activity = domElement.getParentOfType(JpdlNamedActivity.class, false);

    Node selectedNode = findNode(activity);

    if (selectedNode != null) {
      final Graph2D graph = myBuilder.getGraph();

      for (Node n : graph.getNodeArray()) {
        final boolean selected = n.equals(selectedNode);
        graph.setSelected(n, selected);
        if (selected) {
          final YRectangle yRectangle = graph.getRectangle(n);
          if (!myBuilder.getView().getVisibleRect().contains(
            new Rectangle((int)yRectangle.getX(), (int)yRectangle.getY(), (int)yRectangle.getWidth(), (int)yRectangle.getHeight()))) {
            myBuilder.getView().setCenter(graph.getX(n), graph.getY(n));
          }
        }
      }
    }
    myBuilder.getView().updateView();
  }

  @Nullable
  private Node findNode(@Nullable JpdlNamedActivity activity) {
    if (activity == null) return null;

    final Collection<JpdlNode> nodeObjects = myBuilder.getNodeObjects();
    for (JpdlNode jpdlNode : nodeObjects) {
      if (activity.equals(jpdlNode.getIdentifyingElement())) {
        return myBuilder.getNode(jpdlNode);
      }
    }
    return null;
  }

  public GraphBuilder getBuilder() {
    return myBuilder;
  }

  public Overview getOverview() {
    return GraphManager.getGraphManager().createOverview(myBuilder.getView());
  }

  @Override
  public void dispose() {
  }

  public SeamProcessDesignerNavigationProvider getNavigationProvider() {
    return myNavigationProvider;
  }

  public XmlFile getXmlFile() {
    return myXmlFile;
  }

  @Override
  @Nullable
  public Object getData(@NotNull @NonNls final String dataId) {
    return null;
  }

  public JpdlDataModel getDataModel() {
    return myDataModel;
  }

  private class SeamProcessDesignerNavigationProvider extends DomElementNavigationProvider {
    @Override
    public String getProviderName() {
      return JPDL_DESIGNER_NAVIGATION_PROVIDER_NAME;
    }

    @Override
    public void navigate(final DomElement domElement, final boolean requestFocus) {
      setSelectedDomElement(domElement);
    }

    @Override
    public boolean canNavigate(final DomElement domElement) {
      return domElement.isValid();
    }
  }

  private class MyDataProvider implements DataProvider {
    private final Project myProject;
    private final Graph2D myGraph;

    MyDataProvider(final GraphBuilder<JpdlNode, JpdlEdge> builder) {
      myProject = builder.getProject();
      myGraph = builder.getGraph();
    }

    @Override
    @Nullable
    public Object getData(@NotNull @NonNls String dataId) {
      if (CommonDataKeys.PROJECT.is(dataId)) {
        return myProject;
      }
      else if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
        for (final var cursor = myGraph.selectedNodes(); cursor.ok(); cursor.next()) {
          final var jpdlNode = myBuilder.getNodeObject(cursor.node());
          if (jpdlNode != null) {
            return jpdlNode.getIdentifyingElement().getXmlElement();
          }
        }
      }
      return null;
    }
  }
}