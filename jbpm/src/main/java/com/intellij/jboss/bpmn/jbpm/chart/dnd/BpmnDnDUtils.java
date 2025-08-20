package com.intellij.jboss.bpmn.jbpm.chart.dnd;

import com.intellij.jboss.bpmn.jbpm.annotation.AnnotationCoordinator;
import com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartDataModel;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.*;
import com.intellij.openapi.util.Pair;
import com.intellij.util.Function;

final class BpmnDnDUtils {
  static private final AnnotationCoordinator<DefaultNamePrefix>
    namePrefixCoordinator = new AnnotationCoordinator<>(DefaultNamePrefix.class);
  static CreateEventFn createStartEventFn = new CreateEventFn() {
    @Override
    public EventDefinitionExplicitOwner fun(BpmnChartDataModel model) {

      return model.getProcess() == null ? null : model.getProcess().addStartEvent();
    }
  };
  static CreateEventFn createIntermediateThrowEventFn = new CreateEventFn() {
    @Override
    public EventDefinitionExplicitOwner fun(BpmnChartDataModel model) {
      return model.getProcess() == null ? null : model.getProcess().addIntermediateThrowEvent();
    }
  };
  static CreateEventFn createIntermediateCatchEventFn = new CreateEventFn() {
    @Override
    public EventDefinitionExplicitOwner fun(BpmnChartDataModel model) {
      return model.getProcess() == null ? null : model.getProcess().addIntermediateCatchEvent();
    }
  };
  static CreateEventFn createEndEventFn = new CreateEventFn() {
    @Override
    public EventDefinitionExplicitOwner fun(BpmnChartDataModel model) {
      return model.getProcess() == null ? null : model.getProcess().addEndEvent();
    }
  };
  static CreateEventDefinitionFn createCancelEventDefinitionFn = new CreateEventDefinitionFn() {
    @Override
    public TEventDefinition fun(EventDefinitionExplicitOwner event) {
      return event.addCancelEventDefinition();
    }
  };
  static CreateEventDefinitionFn createCompensateEventDefinitionFn = new CreateEventDefinitionFn() {
    @Override
    public TEventDefinition fun(EventDefinitionExplicitOwner event) {
      TCompensateEventDefinition definition = event.addCompensateEventDefinition();
      definition.getWaitForCompletion().setValue(false);
      return definition;
    }
  };
  static CreateEventDefinitionFn createConditionalEventDefinitionFn = new CreateEventDefinitionFn() {
    @Override
    public TEventDefinition fun(EventDefinitionExplicitOwner event) {
      TConditionalEventDefinition definition = event.addConditionalEventDefinition();
      definition.getCondition().setValue("true");
      return definition;
    }
  };
  static CreateEventDefinitionFn createErrorEventDefinitionFn = new CreateEventDefinitionFn() {
    @Override
    public TEventDefinition fun(EventDefinitionExplicitOwner event) {
      return event.addErrorEventDefinition();
    }
  };
  static CreateEventDefinitionFn createEscalationEventDefinitionFn = new CreateEventDefinitionFn() {
    @Override
    public TEventDefinition fun(EventDefinitionExplicitOwner event) {
      return event.addEscalationEventDefinition();
    }
  };
  static CreateEventDefinitionFn createLinkEventDefinitionFn = new CreateEventDefinitionFn() {
    @Override
    public TEventDefinition fun(EventDefinitionExplicitOwner event) {
      return event.addLinkEventDefinition();
    }
  };
  static CreateEventDefinitionFn createMessageEventDefinitionFn = new CreateEventDefinitionFn() {
    @Override
    public TEventDefinition fun(EventDefinitionExplicitOwner event) {
      return event.addMessageEventDefinition();
    }
  };
  static CreateEventDefinitionFn createSignalEventDefinitionFn = new CreateEventDefinitionFn() {
    @Override
    public TEventDefinition fun(EventDefinitionExplicitOwner event) {
      return event.addSignalEventDefinition();
    }
  };
  static CreateEventDefinitionFn createTerminateEventDefinitionFn = new CreateEventDefinitionFn() {
    @Override
    public TEventDefinition fun(EventDefinitionExplicitOwner event) {
      return event.addTerminateEventDefinition();
    }
  };
  static CreateEventDefinitionFn createTimerEventDefinitionFn = new CreateEventDefinitionFn() {
    @Override
    public TEventDefinition fun(EventDefinitionExplicitOwner event) {
      return event.addTimerEventDefinition();
    }
  };

  static TFlowNode createIdsAndNames(BpmnChartDataModel model, TEvent event, TEventDefinition eventDefinition) {
    createIdAndName(model, event);
    if (eventDefinition != null) {
      createIdAndName(model, eventDefinition);
    }
    return event;
  }

  static <T extends TBaseElement> T createIdAndName(BpmnChartDataModel model, T element) {
    DefaultNamePrefix annotation = namePrefixCoordinator.getAnnotation(element.getClass());
    String namePrefix = annotation == null ? "Event" : annotation.value();
    Pair<String, String> idAndName = model.createUniqueNodeIdAndName(namePrefix);
    element.getId().setStringValue(idAndName.first);
    if (element instanceof NameAttributedElement) {
      ((NameAttributedElement)element).getName().setStringValue(idAndName.second);
    }
    return element;
  }

  interface CreateEventFn extends Function<BpmnChartDataModel, EventDefinitionExplicitOwner> {
  }

  interface CreateFlowNodeFn extends Function<BpmnChartDataModel, TFlowNode> {
  }

  interface CreateEventDefinitionFn extends Function<EventDefinitionExplicitOwner, TEventDefinition> {
  }
}
