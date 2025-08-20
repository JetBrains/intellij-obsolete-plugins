package com.intellij.jboss.bpmn.jbpm.model.render;

import com.intellij.jboss.bpmn.jbpm.BpmnUtils;
import com.intellij.jboss.bpmn.jbpm.annotation.AnnotationCoordinator;
import com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartNode;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefinitionKind;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.EventKind;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.EventDefinitionExplicitOwner;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TEvent;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TEventDefinition;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowElement;
import com.intellij.jboss.bpmn.jbpm.providers.DefaultImageProvider;
import com.intellij.jboss.bpmn.jbpm.render.pictures.RenderImage;
import com.intellij.ui.icons.ImageDataByPathLoaderKt;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

public final class EventImageProvider extends DefaultImageProvider<TFlowElement, BpmnChartNode> {
  private static final @NotNull String noDefinitionKind = "Empty";
  private static final @NotNull String multiDefinitionKind = "Multi";
  private static final @NotNull String noEventKind = "Start";
  private final @NotNull AnnotationCoordinator<EventKind> eventAnnotationCoordinator =
    new AnnotationCoordinator<>(EventKind.class);
  private final @NotNull AnnotationCoordinator<DefinitionKind> definitionAnnotationCoordinator =
    new AnnotationCoordinator<>(DefinitionKind.class);

  @Override
  public @NotNull Icon getImage(BpmnChartNode node, RenderImage renderImage) {
    TEvent event = (TEvent)node.getIdentifyingElement();
    EventKind eventKindAnnotation = eventAnnotationCoordinator.getAnnotation(event.getClass());
    String eventKind = eventKindAnnotation == null ? noEventKind : eventKindAnnotation.value();
    String definitionKind = getDefinitionName((EventDefinitionExplicitOwner)event);
    try {
      return Objects.requireNonNull(ImageDataByPathLoaderKt.getReflectiveIcon("JbpmIcons.Bpmn.Events." + eventKind + "_48_" + definitionKind,
                                                                              EventImageProvider.class.getClassLoader()));
    }
    catch (Throwable exc) {
      return Objects.requireNonNull(ImageDataByPathLoaderKt.getReflectiveIcon("JbpmIcons.Bpmn.Events." + eventKind + "_48_" + noDefinitionKind,
                                                                 EventImageProvider.class.getClassLoader()));
    }
  }

  private String getDefinitionName(EventDefinitionExplicitOwner eventDefinitionOwner) {
    List<TEventDefinition> eventDefinitions = BpmnUtils.getEventDefinitions(eventDefinitionOwner);
    if (eventDefinitions.isEmpty()) {
      return noDefinitionKind;
    }
    if (eventDefinitions.size() > 1) {
      return multiDefinitionKind;
    }
    TEventDefinition eventDefinition = eventDefinitions.get(0);
    DefinitionKind definitionKind = definitionAnnotationCoordinator.getAnnotation(eventDefinition.getClass());
    return definitionKind == null ? noDefinitionKind : definitionKind.value();
  }
}
