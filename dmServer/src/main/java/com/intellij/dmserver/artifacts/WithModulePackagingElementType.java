package com.intellij.dmserver.artifacts;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.elements.PackagingElementType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class WithModulePackagingElementType<E extends WithModulePackagingElement> extends PackagingElementType<E> {

  protected WithModulePackagingElementType(@NotNull String id,
                                           @NotNull Supplier<@Nls(capitalization = Nls.Capitalization.Title) String> presentableName) {
    super(id, presentableName);
  }

  public E createFor(@NotNull Module module) {
    Project project = module.getProject();
    E result = createEmpty(project);
    result.setModule(module);
    return result;
  }
}
