package com.intellij.vaadin.actions;

import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateElementActionBase;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.NlsActions;
import com.intellij.psi.PsiDirectory;
import com.intellij.vaadin.framework.VaadinVersionUtil;
import com.intellij.vaadin.VaadinIcons;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

public abstract class VaadinCreateElementActionBase extends CreateElementActionBase {
  protected VaadinCreateElementActionBase(@NlsActions.ActionText String text,
                                          @NlsActions.ActionDescription String description) {
    super(text, description, VaadinIcons.VaadinIcon);
  }

  @Override
  protected boolean isAvailable(DataContext dataContext) {
    if (!super.isAvailable(dataContext)) {
      return false;
    }
    Module module = PlatformCoreDataKeys.MODULE.getData(dataContext);
    if (module == null) return false;

    final IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
    if (view == null) return false;

    if (!VaadinVersionUtil.hasVaadinFramework(module)) return false;

    ProjectFileIndex index = ProjectRootManager.getInstance(module.getProject()).getFileIndex();
    for (PsiDirectory directory : view.getDirectories()) {
      if (index.isUnderSourceRootOfType(directory.getVirtualFile(), JavaModuleSourceRootTypes.SOURCES)) {
        return true;
      }
    }
    return false;
  }
}