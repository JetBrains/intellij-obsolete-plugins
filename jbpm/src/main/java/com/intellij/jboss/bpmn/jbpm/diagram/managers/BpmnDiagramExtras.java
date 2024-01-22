package com.intellij.jboss.bpmn.jbpm.diagram.managers;

import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.extras.DiagramExtras;
import com.intellij.diagram.extras.UmlNodeHighlighter;
import com.intellij.diagram.util.DiagramSelectionService;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.BpmnDiagramEdge;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers.BpmnElementWrapper;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.graph.GraphManager;
import com.intellij.openapi.graph.layout.LayoutOrientation;
import com.intellij.openapi.graph.layout.Layouter;
import com.intellij.openapi.graph.layout.hierarchic.IncrementalHierarchicLayouter;
import com.intellij.openapi.graph.settings.GraphSettings;
import com.intellij.openapi.project.Project;
import com.intellij.pom.references.PomService;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class BpmnDiagramExtras extends DiagramExtras<BpmnElementWrapper<?>> {
  private final UmlNodeHighlighter<BpmnElementWrapper<?>> nodeHighlighter = new BpmnUmlNodeHighlighter();

  @SuppressWarnings("unchecked")
  @Override
  public @NotNull List getExtraActions() {
    return Collections.emptyList(); // todo: DeleteAction ?
  }

  @Override
  public Layouter getCustomLayouter(GraphSettings settings, Project project) {
    final IncrementalHierarchicLayouter layouter = GraphManager.getGraphManager().createIncrementalHierarchicLayouter();
    layouter.setConsiderNodeLabelsEnabled(true);
    layouter.setIntegratedEdgeLabelingEnabled(true);
    layouter.setOrthogonallyRouted(true);
    layouter.setEdgeToEdgeDistance(30);
    layouter.setBackloopRoutingEnabled(true);
    layouter.setLayoutOrientation(LayoutOrientation.TOP_TO_BOTTOM);
    return layouter;
  }

  @Override
  public Object getData(@NotNull String dataId, @NotNull List<DiagramNode<BpmnElementWrapper<?>>> nodes, @NotNull DiagramBuilder builder) {
    if (!CommonDataKeys.NAVIGATABLE.is(dataId) &&
        !CommonDataKeys.PSI_ELEMENT.is(dataId)) {
      return null;
    }

    if (nodes.size() == 1) {
      final BpmnElementWrapper elementWrapper = nodes.get(0).getIdentifyingElement();
      final Object element = elementWrapper.getElement();
      if (!(element instanceof DomElement)) {
        return null;
      }
      return getPsiForDomElement((DomElement)element);
    }

    final List<DiagramEdge<?>> edges = DiagramSelectionService.getInstance().getSelectedEdges(builder);
    if (edges.size() == 1) {
      final DiagramEdge<?> edge = edges.get(0);
      if (!(edge instanceof BpmnDiagramEdge)) {
        return null;
      }
      final DomElement domElement = ((BpmnDiagramEdge)edge).getDefiningElement();
      if (domElement == null) {
        return null;
      }
      return getPsiForDomElement(domElement);
    }
    return null;
  }

  @Override
  public String suggestDiagramFileName(BpmnElementWrapper element) {
    return element.getName();
  }

  @Override
  public UmlNodeHighlighter<BpmnElementWrapper<?>> getNodeHighlighter() {
    return nodeHighlighter;
  }

  @Nullable
  private static Object getPsiForDomElement(DomElement domElement) {
    if (!domElement.isValid()) {
      return null;
    }
    final DomTarget domTarget = DomTarget.getTarget(domElement);
    return domTarget != null ? PomService.convertToPsi(domTarget) : domElement.getXmlElement();
  }
}