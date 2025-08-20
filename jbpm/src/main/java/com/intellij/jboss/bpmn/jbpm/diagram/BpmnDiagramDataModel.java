package com.intellij.jboss.bpmn.jbpm.diagram;

import com.intellij.diagram.*;
import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jbpm.BpmnBundle;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.BpmnDiagramEdge;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.BpmnDiagramNode;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.BpmnEdgeType;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers.Bpmn20DomElementWrapper;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers.BpmnElementWrapper;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers.BpmnUnknownNodeElementWrapper;
import com.intellij.jboss.bpmn.jbpm.diagram.managers.BpmnNodeContentManager;
import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModel;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.*;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.SmartList;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.xml.*;
import com.intellij.util.xml.events.DomEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class BpmnDiagramDataModel extends DiagramDataModel<BpmnElementWrapper<?>> {
  public static final DiagramCategory SUB_FLOWS =
    new DiagramCategory(BpmnBundle.messagePointer("category.name.sub.flows"), JbossJbpmIcons.Bpmn.SubProcess, true, true, true);

  private final Collection<DiagramNode<BpmnElementWrapper<?>>> myNodes = new HashSet<>();
  private final Collection<DiagramEdge<BpmnElementWrapper<?>>> myEdges = new HashSet<>();

  private final BpmnElementWrapper<?> myRootElementWrapper;
  private final DiagramPresentationModel myPresentationModel;

  public BpmnDiagramDataModel(Project project,
                              DiagramProvider<BpmnElementWrapper<?>> provider,
                              BpmnElementWrapper<?> rootElementWrapper,
                              DiagramPresentationModel presentationModel) {
    super(project, provider);

    myRootElementWrapper = rootElementWrapper;
    myPresentationModel = presentationModel;

    setShowDependencies(true);

    DomManager.getDomManager(project).addDomEventListener(new DomEventListener() {
      @Override
      public void eventOccured(@NotNull final DomEvent event) {
        incModificationCount();
      }
    }, this);
    MessageBusConnection connection = getProject().getMessageBus().connect(this);
    connection.setDefaultHandler(this::incModificationCount);
    connection.subscribe(ModuleRootListener.TOPIC);
    connection.subscribe(ModuleListener.TOPIC);
  }

  @Override
  public boolean isDependencyDiagramSupported() {
    return true;
  }

  @Override
  public boolean isPsiListener() {
    return true;
  }

  @Override
  public boolean hasFile(PsiFile file) {
    for (BpmnDomModel model : getProcessDefinitionModels()) {
      final TDefinitions flow = model.getDefinitions();
      final XmlFile xmlFile = DomUtil.getFile(flow);
      if (xmlFile.equals(file)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void removeNode(@NotNull DiagramNode<BpmnElementWrapper<?>> node) {
    // todo see CdiDiagramDataModel
    myNodes.remove(node);

    List<DiagramEdge<BpmnElementWrapper<?>>> toRemove = new SmartList<>();
    for (DiagramEdge<BpmnElementWrapper<?>> edge : myEdges) {
      if (edge.getSource().equals(node) ||
          edge.getTarget().equals(node)) {
        toRemove.add(edge);
      }
    }
    myEdges.removeAll(toRemove);
  }

  @NotNull
  @Override
  public Collection<DiagramNode<BpmnElementWrapper<?>>> getNodes() {
    return myNodes;
  }

  @NotNull
  @Override
  public Collection<DiagramEdge<BpmnElementWrapper<?>>> getEdges() {
    return myEdges;
  }

  @SuppressWarnings("ConstantConditions")
  @NotNull
  @Override
  public String getNodeName(@NotNull DiagramNode<BpmnElementWrapper<?>> node) {
    return node.getTooltip();
  }

  @Override
  public DiagramNode<BpmnElementWrapper<?>> addElement(@Nullable BpmnElementWrapper element) {
    // todo see CdiDiagramDataModel
    return null;
  }

  @Override
  public DiagramEdge<BpmnElementWrapper<?>> createEdge(@NotNull DiagramNode<BpmnElementWrapper<?>> from,
                                                       @NotNull DiagramNode<BpmnElementWrapper<?>> to) {
    if (getBuilder().isPopupMode()) {
      return null;
    }

    final DomElement fromDom = ((Bpmn20DomElementWrapper)from.getIdentifyingElement()).getElement();
    final DomElement toDom = ((Bpmn20DomElementWrapper)to.getIdentifyingElement()).getElement();

    // todo

    return super.createEdge(from, to);
  }

  public List<BpmnDomModel> getProcessDefinitionModels() {
    return myRootElementWrapper.getBpmnModels();
  }

  @Override
  public void refreshDataModel() {
    myNodes.clear();
    myEdges.clear();
    updateDataModel();
  }

  private void updateDataModel() {
    DumbService.getInstance(getProject()).runWhenSmart(() -> {
      // popup mode: "parent" relations, details & event-nodes OFF
      if (getBuilder().isPopupMode()) {
        setShowDependencies(false);
        Objects.requireNonNull(getNodeContentManager()).setCategoryEnabled(BpmnNodeContentManager.DETAILS, false);
      }
      for (final BpmnDomModel model : getProcessDefinitionModels()) {
        processDefinitions(model.getDefinitions());
      }
    });
  }

  private void processDefinitions(TDefinitions definitions) {
    for (TProcess tProcess : definitions.getProcesses()) {
      processTProcess(tProcess);
    }
  }

  private void processTProcess(@NotNull TProcess process) {
    List<TFlowNode> elements = process.getFlowNodes();
    Map<String, BpmnDiagramNode> nodes = new HashMap<>();
    for (TFlowNode element : elements) {
      String nodeId = element.getId().getStringValue();
      if (!StringUtil.isEmptyOrSpaces(nodeId)) {
        nodes.put(nodeId, new BpmnDiagramNode(new Bpmn20DomElementWrapper(element), getProvider()));
      }
    }

    for (TSequenceFlow flow : process.getSequenceFlows()) {
      GenericAttributeValue<TBaseElement> sourceRef = flow.getSourceRef();
      GenericAttributeValue<TBaseElement> targetRef = flow.getTargetRef();

      String sourceId = sourceRef.getStringValue();
      String targetId = targetRef.getStringValue();

      if (StringUtil.isEmptyOrSpaces(sourceId) || StringUtil.isEmptyOrSpaces(targetId)) continue; // todo: process empty(null) node ???

      myEdges.add(new BpmnDiagramEdge(getNodeById(nodes, sourceId), getNodeById(nodes, targetId), flow, BpmnEdgeType.FLOW));
    }
    myNodes.addAll(nodes.values());
  }

  @NotNull
  private BpmnDiagramNode getNodeById(@NotNull Map<String, BpmnDiagramNode> nodes, @NotNull String nodeId) {
    if (!nodes.containsKey(nodeId)) {
      nodes.put(nodeId, new BpmnDiagramNode(new BpmnUnknownNodeElementWrapper(nodeId), getProvider()));
    }
    return nodes.get(nodeId);
  }

  @NotNull
  @Override
  public ModificationTracker getModificationTracker() {
    return this;
  }

  @Override
  public void dispose() {
  }
}
