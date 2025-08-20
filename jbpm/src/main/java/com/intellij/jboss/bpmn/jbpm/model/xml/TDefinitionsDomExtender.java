package com.intellij.jboss.bpmn.jbpm.model.xml;

import com.intellij.jboss.bpmn.jbpm.constants.JbpmNamespaceConstants;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.*;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi.BPMNDiagram;
import com.intellij.util.xml.XmlName;
import com.intellij.util.xml.reflect.DomExtender;
import com.intellij.util.xml.reflect.DomExtensionsRegistrar;
import org.jetbrains.annotations.NotNull;

public class TDefinitionsDomExtender extends DomExtender<TDefinitions> {
  /**
   * @param definitions DOM element where new children may be added to
   * @param registrar   a place to register your own DOM children descriptions
   */
  @Override
  public void registerExtensions(@NotNull TDefinitions definitions, @NotNull DomExtensionsRegistrar registrar) {
    registrar.registerCollectionChildrenExtension(new XmlName("process", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY), TProcess.class);
    registrar
      .registerCollectionChildrenExtension(new XmlName("BPMNDiagram", JbpmNamespaceConstants.JBPM_20_DI_NAMESPACE_KEY), BPMNDiagram.class);

    registrar
      .registerCollectionChildrenExtension(new XmlName("choreography", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY), TChoreography.class);
    registrar.registerCollectionChildrenExtension(new XmlName("collaboration", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TCollaboration.class);
    registrar.registerCollectionChildrenExtension(new XmlName("globalChoreographyTask", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TGlobalChoreographyTask.class);
    registrar.registerCollectionChildrenExtension(new XmlName("globalConversation", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TGlobalConversation.class);

    registrar
      .registerCollectionChildrenExtension(new XmlName("globalTask", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY), TGlobalTask.class);

    registrar.registerCollectionChildrenExtension(new XmlName("globalBusinessRuleTask", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TGlobalBusinessRuleTask.class);
    registrar.registerCollectionChildrenExtension(new XmlName("globalManualTask", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TGlobalManualTask.class);
    registrar.registerCollectionChildrenExtension(new XmlName("globalScriptTask", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TGlobalScriptTask.class);
    registrar.registerCollectionChildrenExtension(new XmlName("globalUserTask", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TGlobalUserTask.class);

    registrar.registerCollectionChildrenExtension(new XmlName("category", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY), TCategory.class);
    registrar.registerCollectionChildrenExtension(new XmlName("correlationProperty", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TCorrelationProperty.class);
    registrar.registerCollectionChildrenExtension(new XmlName("dataStore", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY), TDataStore.class);
    registrar.registerCollectionChildrenExtension(new XmlName("endPoint", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY), TEndPoint.class);
    registrar.registerCollectionChildrenExtension(new XmlName("error", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY), TError.class);
    registrar
      .registerCollectionChildrenExtension(new XmlName("escalation", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY), TEscalation.class);

    registrar.registerCollectionChildrenExtension(new XmlName("eventDefinition", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TEventDefinition.class);
    registrar.registerCollectionChildrenExtension(new XmlName("cancelEventDefinition", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TCancelEventDefinition.class);
    registrar.registerCollectionChildrenExtension(new XmlName("compensateEventDefinition", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TCompensateEventDefinition.class);
    registrar.registerCollectionChildrenExtension(new XmlName("conditionalEventDefinition", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TConditionalEventDefinition.class);
    registrar.registerCollectionChildrenExtension(new XmlName("errorEventDefinition", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TErrorEventDefinition.class);
    registrar.registerCollectionChildrenExtension(new XmlName("escalationEventDefinition", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TEscalationEventDefinition.class);

    registrar.registerCollectionChildrenExtension(new XmlName("linkEventDefinition", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TLinkEventDefinition.class);
    registrar.registerCollectionChildrenExtension(new XmlName("messageEventDefinition", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TMessageEventDefinition.class);
    registrar.registerCollectionChildrenExtension(new XmlName("signalEventDefinition", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TSignalEventDefinition.class);
    registrar.registerCollectionChildrenExtension(new XmlName("terminateEventDefinition", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TTerminateEventDefinition.class);
    registrar.registerCollectionChildrenExtension(new XmlName("timerEventDefinition", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TTimerEventDefinition.class);

    registrar.registerCollectionChildrenExtension(new XmlName("interface", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY), TInterface.class);
    registrar.registerCollectionChildrenExtension(new XmlName("itemDefinition", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TItemDefinition.class);

    registrar.registerCollectionChildrenExtension(new XmlName("message", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY), TMessage.class);
    registrar.registerCollectionChildrenExtension(new XmlName("partnerEntity", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY),
                                                  TPartnerEntity.class);
    registrar
      .registerCollectionChildrenExtension(new XmlName("partnerRole", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY), TPartnerRole.class);
    registrar.registerCollectionChildrenExtension(new XmlName("resource", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY), TResource.class);
    registrar.registerCollectionChildrenExtension(new XmlName("signal", JbpmNamespaceConstants.JBPM_20_NAMESPACE_KEY), TSignal.class);
  }
}
