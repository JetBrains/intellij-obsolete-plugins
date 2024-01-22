package com.intellij.jboss.bpmn.jbpm.model;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.xml.XmlStructureViewBuilderProvider;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.structure.DomStructureViewBuilderProvider;
import org.jetbrains.annotations.NotNull;

public class BpmnStructureViewBuilderProvider implements XmlStructureViewBuilderProvider {
  @Override
  public StructureViewBuilder createStructureViewBuilder(@NotNull XmlFile file) {
    if (BpmnDomModelManager.getInstance(file.getProject()).isBpmnDomModel(file)) {
      return new BpmnStructureViewBuilder(file, DomStructureViewBuilderProvider.DESCRIPTOR);
    }
    return null;
  }
}
