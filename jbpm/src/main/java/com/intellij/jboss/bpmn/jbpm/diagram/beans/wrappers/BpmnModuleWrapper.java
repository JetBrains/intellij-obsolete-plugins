package com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers;

import com.intellij.jboss.bpmn.jbpm.BpmnBundle;
import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModel;
import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModelManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class BpmnModuleWrapper extends BpmnElementWrapper<Module> {

  public BpmnModuleWrapper(@NotNull Module element) {
    super(element);
  }

  @NotNull
  @Override
  public String getName() {
    return BpmnBundle.message("bpmn.module.name", myElement.getName());
  }

  @Override
  public String getFqn() {
    return myElement.getName();
  }

  @NotNull
  @Override
  public List<BpmnDomModel> getBpmnModels() {
    return BpmnDomModelManager.getInstance(myElement.getProject()).getAllModels(myElement);
  }

  @Nullable
  public static BpmnElementWrapper resolveElementByFQN(String fqn, Project project) {
    final Module module = ModuleManager.getInstance(project).findModuleByName(fqn);
    if (module == null) {
      return null;
    }
    return new BpmnModuleWrapper(module);
  }
}
