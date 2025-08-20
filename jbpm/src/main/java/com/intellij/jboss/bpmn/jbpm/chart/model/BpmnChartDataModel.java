package com.intellij.jboss.bpmn.jbpm.chart.model;

import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramPresentationModel;
import com.intellij.jboss.bpmn.jbpm.BpmnUtils;
import com.intellij.jboss.bpmn.jbpm.layout.ChartLayoutCoordinator;
import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModel;
import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModelManager;
import com.intellij.jboss.bpmn.jbpm.model.ChartDataModelImpl;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.*;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc.Bounds;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc.Point;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi.BPMNDiagram;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi.BPMNEdge;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi.BPMNPlane;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi.BPMNShape;
import com.intellij.jboss.bpmn.jbpm.settings.ChartProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartEdgeType.Flow;

public class BpmnChartDataModel extends ChartDataModelImpl<TFlowElement, BpmnChartNode, BpmnChartEdge, BpmnChartSource> {
  private final BpmnDomModel domModel;
  private TProcess process;
  private BPMNDiagram diagram;

  public BpmnChartDataModel(Project project,
                            ChartProvider<TFlowElement> provider,
                            @NotNull BpmnChartSource source,
                            DiagramPresentationModel presentationModel) {
    super(project, provider, source);
    @NotNull PsiFile psiFile = source.getPsiFile();
    assert psiFile instanceof XmlFile && BpmnDomModelManager.getInstance(project).isBpmnDomModel((XmlFile)psiFile);
    domModel = BpmnDomModelManager.getInstance(project).getModel((XmlFile)psiFile);
  }

  @NotNull
  @Override
  public String getNodeName(@NotNull DiagramNode<TFlowElement> n) {
    assert n instanceof BpmnChartNode;
    BpmnChartNode chartNode = (BpmnChartNode)n;
    String value = chartNode.getIdentifyingElement().getName().getStringValue();
    return value == null ? "" : value;
  }

  @Override
  @NotNull
  public BpmnChartEdge createEdge(BpmnChartNode from, BpmnChartNode to) {
    TSequenceFlow sequenceFlow = process.addSequenceFlow();
    sequenceFlow.getSourceRef().setValue(from.getIdentifyingElement());
    sequenceFlow.getTargetRef().setValue(to.getIdentifyingElement());
    String fromId = from.getId();
    String toId = to.getId();
    sequenceFlow.getId().setValue(fromId + "-" + toId);
    BpmnChartEdge chartEdge = addEdge(new BpmnChartEdge(from, to, sequenceFlow, Flow));
    chartEdge.setLayout(createEdgeShape(from.getLayout(), to.getLayout()));
    return chartEdge;
  }

  @Nullable
  @Override
  public BpmnChartNode addElement(@Nullable TFlowElement element) {
    return element != null ? addNode(new BpmnChartNode(getProvider(), element)) : null;
  }

  @Override
  protected void updateDataModel() {
    if (domModel == null) {
      return;
    }
    TDefinitions definitions = domModel.getDefinitions();
    if (definitions.getProcesses().size() == 0) {
      diagram = null;
      process = null;
      return;
    }
    process = definitions.getProcesses().get(0);
    for (BPMNDiagram bpmnDiagram : definitions.getBPMNDiagrams()) {
      TBaseElement key = bpmnDiagram.getBPMNPlane().getBpmnElement().getValue();
      if (process.equals(key)) {
        diagram = bpmnDiagram;
        break;
      }
    }
    processTProcess(process, diagram);
  }

  private void processTProcess(TProcess process, BPMNDiagram diagram) {
    processFlowNodeOwnerRecursively(process);
    for (TSequenceFlow flow : process.getSequenceFlows()) {
      GenericAttributeValue<TBaseElement> sourceRef = flow.getSourceRef();
      GenericAttributeValue<TBaseElement> targetRef = flow.getTargetRef();

      String sourceId = sourceRef.getStringValue();
      String targetId = targetRef.getStringValue();

      BpmnChartNode sourceNode = getNode(sourceId);
      BpmnChartNode targetNode = getNode(targetId);
      if (sourceNode != null && targetNode != null) {
        addEdge(new BpmnChartEdge(sourceNode, targetNode, flow, Flow));
      }
    }
    if (diagram != null) {
      addNodeLayouts();
      addEdgeLayouts();
    }
  }

  private void addNodeLayouts() {
    for (BPMNShape bpmnShape : diagram.getBPMNPlane().getBPMNShapes()) {
      TBaseElement value = bpmnShape.getBpmnElement().getValue();
      if (value == null) {
        continue;
      }
      String id = value.getId().getValue();
      BpmnChartNode node = getNode(id);
      if (node == null) {
        continue;
      }
      node.setLayout(bpmnShape);
    }
  }

  private void addEdgeLayouts() {
    for (BPMNEdge edge : diagram.getBPMNPlane().getBPMNEdges()) {
      String bpmnElement = edge.getBpmnElement().getValue();
      if (bpmnElement == null) {
        continue;
      }
      String[] parts = bpmnElement.split("-");
      if (parts.length != 2) {
        continue;
      }
      BpmnChartEdge chartEdge = getEdge(Pair.create(parts[0], parts[1]));
      if (chartEdge == null) {
        continue;
      }
      chartEdge.setLayout(edge);
    }
  }

  private void processFlowNodeOwnerRecursively(@NotNull FlowElementExplicitOwner owner) {
    for (TFlowNode node : BpmnUtils.getFlowNodes(owner)) {
      addElement(node);
      if (node instanceof FlowElementExplicitOwner) {
        processFlowNodeOwnerRecursively((FlowElementExplicitOwner)node);
      }
    }
  }

  public BPMNShape createNodeShape(@NotNull String id,
                                   @NotNull ChartLayoutCoordinator.Point centerPoint,
                                   @NotNull ChartLayoutCoordinator.Size size) {
    getDiagram(true);
    BPMNShape bpmnShape = diagram.getBPMNPlane().addBPMNShape();
    bpmnShape.getBpmnElement().setStringValue(id);
    Bounds bounds = bpmnShape.getBounds();
    bounds.getX().setValue(centerPoint.x - size.width / 2);
    bounds.getY().setValue(centerPoint.y - size.height / 2);
    bounds.getWidth().setValue(size.width);
    bounds.getHeight().setValue(size.height);

    return bpmnShape;
  }

  @SuppressWarnings("ConstantConditions")
  public BPMNEdge createEdgeShape(BPMNShape from, BPMNShape to) {
    getDiagram(true);
    BPMNEdge bpmnEdge = diagram.getBPMNPlane().addBPMNEdge();
    bpmnEdge.getSourceElement().setValue(from.getId().getStringValue());
    bpmnEdge.getTargetElement().setValue(to.getId().getStringValue());
    bpmnEdge.getBpmnElement().setValue(from.getBpmnElement().getStringValue() + "-" + to.getBpmnElement().getStringValue());
    Point fromPoint = bpmnEdge.addWaypoint();
    fromPoint.getX().setValue(from.getBounds().getX().getValue() + from.getBounds().getWidth().getValue() / 2);
    fromPoint.getY().setValue(from.getBounds().getY().getValue() + from.getBounds().getHeight().getValue() / 2);
    Point toPoint = bpmnEdge.addWaypoint();
    toPoint.getX().setValue(to.getBounds().getX().getValue() + to.getBounds().getWidth().getValue() / 2);
    toPoint.getY().setValue(to.getBounds().getY().getValue() + to.getBounds().getHeight().getValue() / 2);
    return bpmnEdge;
  }

  @Override
  public void dispose() {

  }

  public Pair<String, String> createUniqueNodeIdAndName(String prefix) {
    String idPrefix = prefix.replace(" ", "") + "_";
    String namePrefix = prefix.trim() + " ";
    int index = 1;

    TProcess process = getProcess();
    while (elementTreeContainsId(process, idPrefix + index)) {
      ++index;
    }
    return Pair.create(idPrefix + index, namePrefix + index);
  }

  @Nullable
  public TProcess getProcess() {
    return process;
  }

  @Nullable
  public BPMNDiagram getDiagram(boolean createIfNotExist) {
    if (diagram == null && createIfNotExist) {
      TDefinitions definitions = domModel.getDefinitions();
      diagram = definitions.addBPMNDiagram();
      BPMNPlane bpmnPlane = diagram.addBPMNPlane();
      bpmnPlane.getBpmnElement().setStringValue(process.getId().getStringValue());
    }
    return diagram;
  }

  private static boolean elementTreeContainsId(TBaseElement element, @NotNull String id) {
    if (id.equals(element.getId().getStringValue())) {
      return true;
    }
    for (TBaseElement childElement : DomUtil.getChildrenOfType(element, TBaseElement.class)) {
      if (elementTreeContainsId(childElement, id)) {
        return true;
      }
    }
    return false;
  }
}
