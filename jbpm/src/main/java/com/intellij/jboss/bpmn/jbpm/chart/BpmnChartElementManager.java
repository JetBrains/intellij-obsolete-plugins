package com.intellij.jboss.bpmn.jbpm.chart;

import com.intellij.diagram.AbstractDiagramElementManager;
import com.intellij.diagram.presentation.DiagramState;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowElement;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.ui.SimpleColoredText;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BpmnChartElementManager extends AbstractDiagramElementManager<TFlowElement> {
  @Nullable
  @Override
  public TFlowElement findInDataContext(@NotNull DataContext context) {
    return null;
  }

  @Override
  public boolean isAcceptableAsNode(@Nullable Object element) {
    return element instanceof TFlowElement;
  }

  @Nullable
  @Override
  public String getElementTitle(TFlowElement element) {
    return null;
  }

  @Nullable
  @Override
  public SimpleColoredText getItemName(@Nullable Object element, @NotNull DiagramState presentation) {
    return null;
  }

  @Override
  public @Nullable @Nls String getNodeTooltip(TFlowElement element) {
    return null;
  }
}
