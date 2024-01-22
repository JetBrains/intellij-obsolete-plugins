package com.intellij.jboss.bpmn.jbpm.diagram.beans;

import com.intellij.diagram.DiagramEdgeBase;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramRelationshipInfo;
import com.intellij.diagram.DiagramRelationshipInfoAdapter;
import com.intellij.diagram.presentation.DiagramLineType;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers.BpmnElementWrapper;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.Nullable;

import static com.intellij.diagram.DiagramRelationshipInfo.*;


public class BpmnDiagramEdge extends DiagramEdgeBase<BpmnElementWrapper<?>> {

  private final DomElement myDefiningElement;
  private final BpmnEdgeType myEdgeType;

  public BpmnDiagramEdge(DiagramNode<BpmnElementWrapper<?>> source,
                         DiagramNode<BpmnElementWrapper<?>> target,
                         @Nullable DomElement definingElement,
                         BpmnEdgeType edgeType) {
    super(source, target, getInfo(edgeType));
    myDefiningElement = definingElement;
    myEdgeType = edgeType;
  }

  @Nullable
  public DomElement getDefiningElement() {
    return myDefiningElement;
  }

  public BpmnEdgeType getEdgeType() {
    return myEdgeType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BpmnDiagramEdge that)) return false;
    if (!super.equals(o)) return false;

    if (myEdgeType != that.myEdgeType) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myEdgeType.hashCode();
    return result;
  }

  private static DiagramRelationshipInfo getInfo(final BpmnEdgeType type) {
    return new DiagramRelationshipInfoAdapter.Builder()
      .setName("BPMN_DIAGRAM_EDGE")
      .setLineType(getLineType(type))
      .setSourceArrow(type == BpmnEdgeType.EVENT ? NONE : STANDARD)
      .setTargetArrow(type == BpmnEdgeType.EVENT ? DIAMOND : NONE)
      .create();
  }

  private static DiagramLineType getLineType(BpmnEdgeType type) {
    return type == BpmnEdgeType.SUBFLOW ? DiagramLineType.DOTTED
                                        : DiagramLineType.SOLID;
  }
}
