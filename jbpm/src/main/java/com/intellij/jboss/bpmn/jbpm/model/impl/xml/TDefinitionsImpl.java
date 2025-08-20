package com.intellij.jboss.bpmn.jbpm.model.impl.xml;

import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TDefinitions;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TRootElement;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class TDefinitionsImpl implements TDefinitions {

  @NotNull
  @Override
  public List<TRootElement> getRootElements() {
    return DomUtil.getChildrenOfType(this, TRootElement.class);
  }
}
