package com.intellij.seam.pages.fileEditor;

import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.graph.GraphManager;
import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.graph.builder.GraphBuilderFactory;
import com.intellij.openapi.graph.builder.actions.AbstractGraphAction;
import com.intellij.openapi.graph.builder.dnd.ProjectViewDnDHelper;
import com.intellij.openapi.graph.services.GraphSelectionService;
import com.intellij.openapi.graph.view.Graph2D;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.graph.view.Overview;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.pages.graph.PagesDataModel;
import com.intellij.seam.pages.graph.PagesPresentationModel;
import com.intellij.seam.pages.graph.beans.BasicPagesEdge;
import com.intellij.seam.pages.graph.beans.BasicPagesNode;
import com.intellij.seam.pages.graph.dnd.PagesProjectViewDnDSupport;
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
import java.util.List;

public class PagesGraphComponent extends JPanel implements DataProvider, Disposable {
  @NonNls private static final String SEAM_PAGES_GRAPH_NAVIGATION_PROVIDER_NAME = "SEAM_PAGES_GRAPH_NAVIGATION_PROVIDER_NAME";

  @NonNls private final SeamPagesGraphNavigationProvider myNavigationProvider = new SeamPagesGraphNavigationProvider();

  private final GraphBuilder<BasicPagesNode, BasicPagesEdge> myBuilder;
  private final XmlFile myXmlFile;
  private final PagesDataModel myDataModel;

  public PagesGraphComponent(final XmlFile xmlFile) {
    myXmlFile = xmlFile;
    final Project project = xmlFile.getProject();

    final Graph2D graph = GraphManager.getGraphManager().createGraph2D();
    final Graph2DView view = GraphManager.getGraphManager().createGraph2DView();
    myDataModel = new PagesDataModel(xmlFile);
    PagesPresentationModel presentationModel = new PagesPresentationModel(graph, project);

    myBuilder = GraphBuilderFactory.getInstance(project).createGraphBuilder(graph, view, myDataModel, presentationModel);
    Disposer.register(this, myBuilder);
    JComponent graphComponent = myBuilder.getView().getJComponent();

    setLayout(new BorderLayout());
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
      ActionPlaces.TOOLBAR, AbstractGraphAction.getCommonToolbarActions(), true);
    toolbar.setTargetComponent(graphComponent);

    add(toolbar.getComponent(), BorderLayout.NORTH);
    add(graphComponent, BorderLayout.CENTER);

    myBuilder.initialize();

    addDnDSupport(xmlFile, myBuilder);

    DomManager.getDomManager(myBuilder.getProject()).addDomEventListener(new DomEventListener() {
      @Override
      public void eventOccured(@NotNull final DomEvent event) {
        if (isShowing()) {
          myBuilder.queueUpdate();
        }
      }
    }, this);
  }

  private static void addDnDSupport(final XmlFile xmlFile, final GraphBuilder<BasicPagesNode, BasicPagesEdge> builder) {
    final WebFacet webFacet = WebUtil.getWebFacet(xmlFile.getContainingFile());
    if (webFacet != null) {
      ProjectViewDnDHelper.getInstance(xmlFile.getProject())
        .addProjectViewDnDSupport(builder, new PagesProjectViewDnDSupport(xmlFile, builder, webFacet));
    }
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
    //if (domElement == null) return;
    //
    //final SeamPagesDomElement pageflowDomElement = domElement.getParentOfType(SeamPagesDomElement.class, false);
    //if (pageflowDomElement == null) return;
    //
    //final Node selectedNode = myBuilder.getNode(pageflowDomElement);
    //
    //if (selectedNode != null) {
    //  final Graph2D graph = myBuilder.getGraph();
    //
    //  for (Node n : graph.getNodeArray()) {
    //    final boolean selected = n.equals(selectedNode);
    //    graph.setSelected(n, selected);
    //    if (selected) {
    //      final YRectangle yRectangle = graph.getRectangle(n);
    //      if (!myBuilder.getView().getVisibleRect().contains(
    //        new Rectangle((int)yRectangle.getX(), (int)yRectangle.getY(), (int)yRectangle.getWidth(), (int)yRectangle.getHeight()))) {
    //        myBuilder.getView().setCenter(graph.getX(n), graph.getY(n));
    //      }
    //    }
    //  }
    //}
    //myBuilder.getView().updateView();
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

  private class SeamPagesGraphNavigationProvider extends DomElementNavigationProvider {
    @Override
    public String getProviderName() {
      return SEAM_PAGES_GRAPH_NAVIGATION_PROVIDER_NAME;
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

  public SeamPagesGraphNavigationProvider getNavigationProvider() {
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

  public PagesDataModel getDataModel() {
    return myDataModel;
  }

  }