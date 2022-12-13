package com.intellij.dmserver.artifacts;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class WithModulePackagingElement extends CompositePackagingElement<WithModuleElementState> {
  private final Project myProject;
  private String myModuleName;

  public WithModulePackagingElement(@NotNull PackagingElementType<? extends WithModulePackagingElement> type,
                                    @NotNull Project project,
                                    @Nullable Module module) {
    super(type);
    myProject = project;
    myModuleName = module == null ? null : module.getName();
  }

  @Override
  public void loadState(@NotNull WithModuleElementState state) {
    myModuleName = state.getModuleId();
  }

  @Override
  public WithModuleElementState getState() {
    WithModuleElementState result = new WithModuleElementState();
    result.setModuleId(myModuleName);
    return result;
  }

  public Project getProject() {
    return myProject;
  }

  public String getModuleName() {
    return myModuleName;
  }

  public void setModulePointer(@NotNull ModulePointer modulePointer) {
    myModuleName = modulePointer.getModuleName();
  }

  public void setModule(@NotNull Module module) {
    myModuleName = module.getName();
  }


  @Override
  public boolean isEqualTo(@NotNull PackagingElement<?> element) {
    return getClass() == element.getClass() && myModuleName != null
           && myModuleName.equals(((WithModulePackagingElement)element).myModuleName);
  }

  public Module findModule() {
    if (myModuleName == null) {
      return null;
    }
    for (Module next : new DefaultModulesProvider(myProject).getModules()) {
      if (myModuleName.equals(next.getName())) {
        return next;
      }
    }
    return null;
  }
}
