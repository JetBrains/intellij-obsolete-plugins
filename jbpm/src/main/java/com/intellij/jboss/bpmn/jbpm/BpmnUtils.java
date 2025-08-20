package com.intellij.jboss.bpmn.jbpm;

import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.*;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi.BPMNShape;
import com.intellij.util.Processor;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;

public final class BpmnUtils {
  public static void processAllElements(@NotNull final Collection<? extends Bpmn20DomElement> initial,
                                        final Processor<? super TBaseElement> processor) {
    processAllElements(initial, processor, TBaseElement.class);
  }

  public static <T extends TBaseElement> void processAllElements(@NotNull final Collection<? extends Bpmn20DomElement> initial,
                                                                 final Processor<? super T> processor, Class<T> clazz) {
    final ArrayDeque<Bpmn20DomElement> queue = new ArrayDeque<>(initial);
    while (!queue.isEmpty()) {
      Bpmn20DomElement element = queue.removeFirst();
      if (clazz.isAssignableFrom(element.getClass())) {
        //noinspection unchecked
        if (!processor.process((T)element)) return;
      }
      final List<TBaseElement> children = DomUtil.getDefinedChildrenOfType(element, TBaseElement.class, true, false);
      queue.addAll(children);
    }
  }

  public static List<TFlowElement> getFlowElements(FlowElementExplicitOwner owner) {
    return DomUtil.getChildrenOfType(owner, TFlowElement.class);
  }

  public static List<TEventDefinition> getEventDefinitions(EventDefinitionExplicitOwner owner) {
    return DomUtil.getChildrenOfType(owner, TEventDefinition.class);
  }

  public static List<TFlowNode> getFlowNodes(FlowElementExplicitOwner owner) {
    return DomUtil.getChildrenOfType(owner, TFlowNode.class);
  }


  public static Dimension getShapeSize(BPMNShape shape) {
    return new JBDimension(
      (int)getAttributeValue(shape.getBounds().getWidth()),
      (int)getAttributeValue(shape.getBounds().getHeight()));
  }

  public static double getAttributeValue(GenericAttributeValue<Double> attributeValue) {
    if (attributeValue.getStringValue() == null) {
      return 0;
    }
    Double result = attributeValue.getValue();
    return result == null ? 0 : result;
  }
}
