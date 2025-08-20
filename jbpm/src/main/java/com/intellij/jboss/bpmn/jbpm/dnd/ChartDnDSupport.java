package com.intellij.jboss.bpmn.jbpm.dnd;

import com.intellij.ide.dnd.*;
import com.intellij.jboss.bpmn.jbpm.JbpmGraphApiBundle;
import com.intellij.jboss.bpmn.jbpm.dnd.node.ChartDnDJointNode;
import com.intellij.jboss.bpmn.jbpm.dnd.node.ChartDnDLeafNode;
import com.intellij.jboss.bpmn.jbpm.dnd.node.ChartDnDNode;
import com.intellij.jboss.bpmn.jbpm.layout.ChartLayoutCoordinator;
import com.intellij.jboss.bpmn.jbpm.model.ChartDataModel;
import com.intellij.jboss.bpmn.jbpm.model.ChartNode;
import com.intellij.jboss.bpmn.jbpm.render.size.ChartNodeSizeEnhancer;
import com.intellij.jboss.bpmn.jbpm.render.size.RenderDefaultSize;
import com.intellij.jboss.bpmn.jbpm.ui.ChartBuilder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.graph.builder.dnd.DraggedComponentsTree;
import com.intellij.openapi.graph.builder.dnd.GraphDnDSupport;
import com.intellij.openapi.graph.builder.dnd.SimpleDnDPanel;
import com.intellij.openapi.graph.builder.dnd.SimpleGraphDnDStructure;
import com.intellij.openapi.graph.builder.util.NodeFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ChartDnDSupport<T, Model extends ChartDataModel<T>, Node extends ChartNode<T>, NodeT extends Enum<NodeT> & ChartDnDNodeDefinition<T, Node, Model>>
  implements GraphDnDSupport<Node, NodeT> {
  protected final @NotNull Model model;
  protected final @NotNull ChartBuilder<T> builder;
  private final NodeT @NotNull [] values;
  private final boolean startInWCA;

  public ChartDnDSupport(@NotNull Model model, @NotNull ChartBuilder<T> builder, NodeT @NotNull [] values, boolean wca) {
    this.model = model;
    this.builder = builder;
    this.values = values;
    startInWCA = wca;
  }

  @Override
  public Map<NodeT, Pair<String, Icon>> getDnDActions() {
    Map<NodeT, Pair<String, Icon>> nodes = new LinkedHashMap<>();
    for (NodeT nodeType : values) {
      nodes.put(nodeType, Pair.create(nodeType.getName(), nodeType.getIcon()));
    }
    return nodes;
  }

  @Nullable
  @Override
  public Node drop(NodeT t) {
    return null;
  }

  private void createNodeLayout(Node node, ChartLayoutCoordinator.Point point) {
    ChartLayoutCoordinator chartLayoutCoordinator = builder.getChartLayoutCoordinator();
    if (chartLayoutCoordinator == null) {
      return;
    }
    String fqn = builder.getProvider().getVfsResolver().getQualifiedName(node.getIdentifyingElement());
    RenderDefaultSize size = builder
      .getProvider()
      .getExtras()
      .getRenderSizeCoordinator()
      .getAnnotation(node.getClassesWithAnnotationsForRendering());
    ChartLayoutCoordinator.NodeLayout nodeLayout = ChartLayoutCoordinator.NodeLayout.createByCenterPoint(
      new ChartLayoutCoordinator.Size(size.width(), size.height()),
      point);

    Runnable createLayoutAction = chartLayoutCoordinator.getChangeNodeLayoutAction(
      fqn,
      nodeLayout,
      ChartNodeSizeEnhancer.enhancerForNode(node));
    if (createLayoutAction != null) {
      createLayoutAction.run();
    }
  }

  @Nullable
  public Node drop(final NodeT nodeType, ChartLayoutCoordinator.Point point) {
    Function<NodeT, Node> fn = t -> {
      Function<Model, Node> createFunction = nodeType.getCreateFunction();
      if (createFunction == null) {
        return null;
      }
      Node node = createFunction.fun(model);
      createNodeLayout(node, point);
      return node;
    };
    if (!startInWCA) {
      return fn.fun(nodeType);
    }

    VirtualFile virtualFile = builder.getEditorFile();
    PsiFile file = virtualFile == null ? null : PsiManager.getInstance(builder.getProject()).findFile(virtualFile);
    String name = JbpmGraphApiBundle.message("chart.add.node", builder.getProvider().getChartName());
    return WriteCommandAction.writeCommandAction(builder.getProject(), file).withName(name).compute(() ->
                                                                                                      fn.fun(nodeType));
  }

  public SimpleGraphDnDStructure createStructure(final Project project) {
    List<ChartDnDNode> result = new ArrayList<>();
    List<ChartDnDNode> nodesToAdd = result;

    for (NodeT node : values) {
      if (!node.isLeafNode()) {
        nodesToAdd = new ArrayList<>();
        result.add(new ChartDnDJointNode(project, node.getName(), node.getIcon(), nodesToAdd));
        continue;
      }
      nodesToAdd.add(new ChartDnDLeafNode<>(project, node, node.getName(), node.getIcon()) {
        @Override
        public boolean canStartDragging() {
          return ChartDnDSupport.this.canStartDragging(getValue());
        }
      });
    }
    final ChartDnDNode[] nodes = result.toArray(new ChartDnDNode[0]);

    return new SimpleGraphDnDStructure(project) {
      @Override
      protected SimpleNode[] getChildren(Project project) {
        return nodes;
      }
    };
  }

  public SimpleDnDPanel<?> createDnDActions(final Project project, ChartBuilder<?> builder) {
    final SimpleGraphDnDStructure graphDnDStructure = createStructure(project);
    final SimpleDnDPanel<T> simpleDnDPanel = new SimpleDnDPanel<>(graphDnDStructure, builder);

    final DnDManager dndManager = DnDManager.getInstance();
    dndManager.registerSource(new ChartDnDSource(simpleDnDPanel.getTree()), simpleDnDPanel.getTree());
    dndManager.registerTarget(new ChartDnDTarget(builder), builder.getView().getJComponent());

    return simpleDnDPanel;
  }

  private static class ChartDnDSource implements DnDSource {
    private final DraggedComponentsTree myTree;

    ChartDnDSource(DraggedComponentsTree tree) {
      myTree = tree;
    }

    @Override
    public boolean canStartDragging(DnDAction action, @NotNull Point dragOrigin) {
      SimpleNode node = myTree.getSelectedNode();
      if (!(node instanceof ChartDnDLeafNode)) {
        return false;
      }
      return ((ChartDnDLeafNode<?>)node).canStartDragging();
    }

    @Override
    public DnDDragStartBean startDragging(DnDAction action, @NotNull Point point) {
      SimpleNode node = myTree.getSelectedNode();
      if (!(node instanceof ChartDnDLeafNode)) {
        return null;
      }
      return new DnDDragStartBean(((ChartDnDLeafNode<?>)myTree.getSelectedNode()).getValue());
    }
  }

  private final class ChartDnDTarget implements DnDTarget {
    private final ChartBuilder<?> builder;

    private ChartDnDTarget(ChartBuilder<?> builder) {
      this.builder = builder;
    }

    @Override
    public boolean update(final DnDEvent aEvent) {
      aEvent.setDropPossible(true);
      return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void drop(final DnDEvent aEvent) {
      Object attachedObject = aEvent.getAttachedObject();
      assert attachedObject instanceof Enum;
      assert attachedObject instanceof ChartDnDNodeDefinition;
      @SuppressWarnings("unchecked") final NodeT attachedNodeT = (NodeT)attachedObject;
      Point point = aEvent.getPoint();
      Node chartNode = ChartDnDSupport.this.drop(attachedNodeT, new ChartLayoutCoordinator.Point(point.getX(), point.getY()));
      if (chartNode != null) {
        final String nodeName = builder.getGraphDataModel().getNodeName(chartNode);
        NodeFactory.getInstance().createDraggedNode(builder, chartNode, nodeName, point);
      }
    }
  }
}
