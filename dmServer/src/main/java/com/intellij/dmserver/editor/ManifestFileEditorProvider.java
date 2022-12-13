package com.intellij.dmserver.editor;

import com.intellij.dmserver.facet.DMBundleFacet;
import com.intellij.facet.FacetFinder;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.lang.manifest.ManifestFileType;

public class ManifestFileEditorProvider implements FileEditorProvider, DumbAware {

  @NonNls private static final String TYPE_ID = "dm-manifest-editor";

  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
    if (file.isDirectory() || !file.isValid()) {
      return false;
    }

    FileType fileType = file.getFileType();
    if (!(fileType instanceof ManifestFileType)) {
      return false;
    }

    return findBundleFacet(project, file) != null;
  }

  private static DMBundleFacet findBundleFacet(Project project, VirtualFile file) {
    FacetFinder facetFinder = FacetFinder.getInstance(project);
    return facetFinder.findFacet(file, DMBundleFacet.ID);
  }

  @NotNull
  @Override
  public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
    return new ManifestFileEditor(project, file);
  }

  @NotNull
  @Override
  public String getEditorTypeId() {
    return TYPE_ID;
  }

  @NotNull
  @Override
  public FileEditorPolicy getPolicy() {
    return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
  }
}
