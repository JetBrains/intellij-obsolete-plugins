package com.intellij.jboss.bpmn.jbpm.model.impl;

import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModel;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TDefinitions;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.model.impl.DomModelImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class BpmnDomModelImpl extends DomModelImpl<TDefinitions> implements BpmnDomModel {

  public BpmnDomModelImpl(@NotNull DomFileElement<TDefinitions> mergedModel, @NotNull Set<XmlFile> configFiles) {
    super(mergedModel, configFiles);
    assert configFiles.size() == 1 : configFiles.size();
  }

  @NotNull
  @Override
  public TDefinitions getDefinitions() {
    return getMergedModel();
  }

  @NotNull
  @Override
  public XmlFile getFlowFile() {
    final Set<XmlFile> files = getConfigFiles();
    return files.iterator().next();
  }
}
