package com.intellij.jboss.bpmn.jbpm.chart.model;

import com.intellij.jboss.bpmn.jbpm.model.ChartNode;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowElement;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi.BPMNShape;
import com.intellij.jboss.bpmn.jbpm.settings.ChartProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;

public class BpmnChartNode extends ChartNode<TFlowElement> {
  @NotNull private final TFlowElement element;
  @Nullable private BPMNShape layout;

  public BpmnChartNode(ChartProvider<TFlowElement> provider, @NotNull TFlowElement element) {
    super(provider);
    this.element = element;
  }

  @Override
  public String getId() {
    return element.getId().getStringValue();
  }

  @NotNull
  @Override
  public Collection<Class<?>> getClassesWithAnnotationsForRendering() {
    return Collections.singletonList(element.getClass());
  }

  @Override
  public void removeSelf() {
    removeLayout();
    element.undefine();
  }

  @Nullable
  @Override
  public String getTooltip() {
    return element.getName().getStringValue();
  }

  @Override
  public @Nullable Icon getIcon() {
    return null;
  }

  @NotNull
  @Override
  public TFlowElement getIdentifyingElement() {
    return element;
  }

  @Nullable
  public BPMNShape getLayout() {
    return layout;
  }

  public void setLayout(@Nullable BPMNShape layout) {
    this.layout = layout;
  }

  public void removeLayout() {
    if (layout != null) {
      layout.undefine();
      layout = null;
    }
  }
}
