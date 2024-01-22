package com.intellij.jboss.bpmn.jbpm.diagram.managers;

import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.extras.UmlNodeHighlighter;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers.BpmnElementWrapper;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public final class BpmnUmlNodeHighlighter implements UmlNodeHighlighter<BpmnElementWrapper<?>> {
  @Override
  public @NotNull List<DiagramNode<BpmnElementWrapper<?>>> onNodeSelected(@NotNull List<DiagramNode<BpmnElementWrapper<?>>> selectedNodes,
                                                                          @NotNull DiagramDataModel<BpmnElementWrapper<?>> model) {
    if (selectedNodes.size() != 1) {
      return Collections.emptyList();
    }

    List<DiagramNode<BpmnElementWrapper<?>>> highlightNodes = new SmartList<>();
    collectNodes(highlightNodes,
                 new HashSet<>(),
                 model,
                 selectedNodes.isEmpty() ? null : selectedNodes.get(0));

    return highlightNodes;
  }

  @Override
  public void selectionChanged(@NotNull DiagramBuilder builder) {
  }

  private static void collectNodes(List<DiagramNode<BpmnElementWrapper<?>>> highlightNodes,
                                   Collection<DiagramEdge<BpmnElementWrapper<?>>> visitedEdges,
                                   DiagramDataModel<BpmnElementWrapper<?>> model,
                                   DiagramNode<BpmnElementWrapper<?>> node) {
    List<DiagramEdge<BpmnElementWrapper<?>>> outgoingEdges = getOutgoingEdges(model, node);
    if (outgoingEdges.isEmpty()) {
      highlightNodes.add(node);
      return;
    }

    for (DiagramEdge<BpmnElementWrapper<?>> edge : outgoingEdges) {
      if (!visitedEdges.contains(edge)) {
        highlightNodes.add(node);
        visitedEdges.add(edge);
        collectNodes(highlightNodes, visitedEdges, model, edge.getTarget());
      }
    }
  }

  private static List<DiagramEdge<BpmnElementWrapper<?>>> getOutgoingEdges(DiagramDataModel<BpmnElementWrapper<?>> model,
                                                                           DiagramNode<BpmnElementWrapper<?>> node) {
    return ContainerUtil.findAll(model.getEdges(), edge -> edge.getSource().equals(node));
  }
}
