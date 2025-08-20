package com.intellij.jboss.bpmn.jbpm.model;

import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramNode;
import com.intellij.jboss.bpmn.jbpm.JbpmGraphApiBundle;
import com.intellij.jboss.bpmn.jbpm.settings.ChartProvider;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ChartDataModelImpl<T, Node extends ChartNode<T>, Edge extends ChartEdge<T>, Source extends ChartSource>
  extends ChartDataModel<T> {
  private final Map<String, Node> nodes = new HashMap<>();
  private final HashMap<Pair<String, String>, Edge> edges = new HashMap<>();
  @NotNull final private Source source;

  public ChartDataModelImpl(Project project, ChartProvider<T> provider, @NotNull Source source) {
    super(project, provider);
    this.source = source;
  }

  @NotNull
  @Override
  public Collection<Node> getNodes() {
    return nodes.values();
  }

  @NotNull
  @Override
  public Collection<Edge> getEdges() {
    return edges.values();
  }

  @Override
  public void refreshDataModel() {
    nodes.clear();
    edges.clear();
    updateDataModel();
  }

  protected Node addNode(Node node) {
    String nodeId = node.getId();
    if (!StringUtil.isNotEmpty(nodeId)) {
      return null;
    }
    assert !nodes.containsKey(nodeId);
    nodes.put(nodeId, node);
    return node;
  }

  protected Edge addEdge(Edge edge) {
    ChartNode<T> source = edge.getSource();
    ChartNode<T> target = edge.getTarget();
    edges.put(Pair.create(source.getId(), target.getId()), edge);
    return edge;
  }

  @Override
  public void removeNode(@NotNull DiagramNode<T> node) {
    @SuppressWarnings("unchecked") Runnable action = getRemoveNodeAction((Node)node);
    if (action == null) {
      return;
    }
    action.run();
  }

  @NotNull
  @Override
  public final Edge createEdge(@NotNull DiagramNode<T> from, @NotNull DiagramNode<T> to) {
    assert from instanceof ChartNode;
    assert to instanceof ChartNode;

    String name = JbpmGraphApiBundle.message("chart.add.edge", getProvider().getChartName());
    return WriteCommandAction.writeCommandAction(getProject(), getSource().getPsiFile()).withName(name)
      .compute(() -> {
        //noinspection unchecked
        return createEdge((Node)from, (Node)to);
      });
  }

  public abstract Edge createEdge(Node from, Node to);

  @Override
  public void removeEdge(@NotNull DiagramEdge<T> edge) {
    //noinspection unchecked
    Runnable action = getRemoveEdgeAction((Edge)edge);
    if (action == null) {
      return;
    }
    String name = JbpmGraphApiBundle.message("chart.remove.edge", getProvider().getChartName());
    WriteCommandAction.writeCommandAction(getProject(), source.getPsiFile()).withName(name)
      .run(() -> action.run());
  }

  protected Runnable getRemoveNodeAction(Node node) {
    String nodeId = node.getId();
    if (!StringUtil.isNotEmpty(nodeId)) {
      return null;
    }
    nodes.remove(nodeId);
    List<Pair<String, String>> edgeKeys = JBIterable.from(this.edges.keySet()).filter(
      pair -> nodeId.equals(pair.first) || nodeId.equals(pair.second)).toList();
    final List<Edge> edges = JBIterable.from(edgeKeys).transform(pair -> this.edges.get(pair)).toList();
    for (Pair<String, String> pair : edgeKeys) {
      this.edges.remove(pair);
    }
    return () -> {
      for (Edge edge : edges) {
        edge.removeSelf();
      }
      node.removeSelf();
    };
  }

  protected Runnable getRemoveEdgeAction(Edge edge) {
    ChartNode<T> source = edge.getSource();
    ChartNode<T> target = edge.getTarget();
    edges.remove(Pair.create(source.getId(), target.getId()));
    return () -> edge.removeSelf();
  }

  protected abstract void updateDataModel();

  public Edge getEdge(Pair<String, String> pair) {
    return edges.get(pair);
  }

  public Node getNode(String fqn) {
    return nodes.get(fqn);
  }

  @NotNull
  protected Source getSource() {
    return source;
  }

  @NotNull
  @Override
  public ModificationTracker getModificationTracker() {
    return this;
  }
}
