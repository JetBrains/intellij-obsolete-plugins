package com.intellij.lang.puppet.psi.references;

import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.project.PuppetEntity;
import com.intellij.lang.puppet.project.PuppetProjectManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ResolveScopeProvider;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetResolveScopeProvider extends ResolveScopeProvider {
  @Override
  public @Nullable GlobalSearchScope getResolveScope(@NotNull VirtualFile file, @NotNull Project project) {
    if (FileTypeRegistry.getInstance().isFileOfType(file, PuppetFileType.INSTANCE)) {
      PuppetEntity entity = PuppetProjectManager.getInstance(project).findModuleOrEnvironmentForFile(file);
      if (entity != null) {
        return entity.getResolveScope();
      }
    }
    return null;
  }
}
