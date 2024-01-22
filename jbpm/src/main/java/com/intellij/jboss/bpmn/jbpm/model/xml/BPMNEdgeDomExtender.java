package com.intellij.jboss.bpmn.jbpm.model.xml;

import com.intellij.jboss.bpmn.jbpm.constants.JbpmNamespaceConstants;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndc.Point;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi.BPMNEdge;
import com.intellij.util.xml.XmlName;
import com.intellij.util.xml.reflect.DomExtender;
import com.intellij.util.xml.reflect.DomExtensionsRegistrar;
import org.jetbrains.annotations.NotNull;

public class BPMNEdgeDomExtender extends DomExtender<BPMNEdge> {

  @Override
  public void registerExtensions(@NotNull BPMNEdge plane, @NotNull DomExtensionsRegistrar registrar) {
    registrar.registerCollectionChildrenExtension(new XmlName("waypoint", JbpmNamespaceConstants.OMG_DI_NAMESPACE_KEY), Point.class);
  }
}
