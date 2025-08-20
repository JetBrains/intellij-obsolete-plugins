package com.intellij.jboss.bpmn.jpdl.model.xml.converters;

import com.intellij.jboss.bpmn.jpdl.model.xml.ProcessDefinition;
import com.intellij.jboss.bpmn.jpdl.utils.JpdlCommonUtils;
import com.intellij.openapi.module.Module;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class ProcessDefinitionConverter extends ResolvingConverter<ProcessDefinition> {

  @Override
  @NotNull
  public Collection<ProcessDefinition> getVariants(final ConvertContext context) {
    return getProcessDefinitions(context);
  }

  @Override
  public ProcessDefinition fromString(@Nullable @NonNls final String s, final ConvertContext context) {
    if (s == null) return null;

    final List<ProcessDefinition> processDefinitions = getProcessDefinitions(context);
    for (ProcessDefinition definition : processDefinitions) {
      if (s.equals(definition.getName().getStringValue())) {
        return definition;
      }
    }

    return null;
  }

  @Override
  public String toString(@Nullable final ProcessDefinition processDefinition, final ConvertContext context) {
    return processDefinition == null ? null : processDefinition.getName().getStringValue();
  }

  @NotNull
  private static List<ProcessDefinition> getProcessDefinitions(final ConvertContext context) {
    final Module module = context.getModule();

    return JpdlCommonUtils.getProcessDefinitions(module);
  }
}

