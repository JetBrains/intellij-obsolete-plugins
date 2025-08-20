package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi.BPMNDiagram;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

//import com.intellij.jboss.jbpm.model.bpmndi.BPMNDiagram;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tDefinitions interface.
 */
public interface TDefinitions extends Bpmn20DomElement {

  @NotNull
  GenericAttributeValue<String> getName();

  @NotNull
  @SubTagList("process")
  List<TProcess> getProcesses();

  @NotNull
  @Required
  GenericAttributeValue<String> getTargetNamespace();

  @NotNull
  GenericAttributeValue<String> getExpressionLanguage();

  @NotNull
  GenericAttributeValue<String> getTypeLanguage();

  @NotNull
  GenericAttributeValue<String> getExporter();

  @NotNull
  GenericAttributeValue<String> getExporterVersion();

  @NotNull
  List<TImport> getImports();

  @NotNull
  List<TExtension> getExtensions();

  @NotNull
  List<TRootElement> getRootElements();

  @NotNull
  List<BPMNDiagram> getBPMNDiagrams();

  @NotNull
  BPMNDiagram addBPMNDiagram();

  @NotNull
  List<TRelationship> getRelationships();
}
