package com.intellij.seam.dependencies;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class SeamDependenciesEditorProvider implements FileEditorProvider, DumbAware {
  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
    return file.getFileSystem() instanceof SeamDependenciesVirtualFileSystem;
  }

  @Override
  @NotNull
  public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
    final String moduleName = file.getName();
    final Module moduleByName = ModuleManager.getInstance(project).findModuleByName(moduleName);
    return new SeamDependenciesFileEditor(moduleByName);
  }

  @Override
  @NotNull
  @NonNls
  public String getEditorTypeId() {
    return "SeamDependenciesFileEditor";
  }

  @Override
  @NotNull
  public FileEditorPolicy getPolicy() {
    return FileEditorPolicy.NONE;
  }
}
