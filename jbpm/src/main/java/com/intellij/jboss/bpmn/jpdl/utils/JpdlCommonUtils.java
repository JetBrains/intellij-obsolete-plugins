package com.intellij.jboss.bpmn.jpdl.utils;

import com.intellij.jboss.bpmn.jpdl.model.JpdlDomModelManager;
import com.intellij.jboss.bpmn.jpdl.model.JpdlModel;
import com.intellij.jboss.bpmn.jpdl.model.xml.ProcessDefinition;
import com.intellij.openapi.module.Module;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class JpdlCommonUtils {

  @NotNull
  public static List<ProcessDefinition> getProcessDefinitions(@Nullable Module module) {
    if (module == null) return Collections.emptyList();

    final List<JpdlModel> models = JpdlDomModelManager.getInstance(module.getProject()).getAllModels(module);
    return ContainerUtil.map(models, processModel -> processModel.getRoots().get(0).getRootElement());
  }

  @NotNull
  public static List<String> getProcessNames(@Nullable Module module) {
    return ContainerUtil.mapNotNull(getProcessDefinitions(module), processDefinition -> processDefinition.getName().getStringValue());
  }
}
