package com.intellij.jboss.bpmn.jpdl.inspections;

import com.intellij.jboss.bpmn.jpdl.model.xml.ProcessDefinition;
import com.intellij.jboss.bpmn.jpdl.resources.messages.JpdlBundle;
import com.intellij.util.xml.highlighting.BasicDomElementsInspection;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class JpdlModelInspection extends BasicDomElementsInspection<ProcessDefinition> {

  public JpdlModelInspection() {
    super(ProcessDefinition.class);
  }

  @Override
  @NotNull
  public String getGroupDisplayName() {
    return JpdlBundle.message("model.inspection.group.name");
  }

  @Override
  @NotNull
  @NonNls
  public String getShortName() {
    return "JpdlModelInspection";
  }
}
