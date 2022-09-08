package com.intellij.seam.dependencies;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.seam.SeamIcons;
import com.intellij.seam.constants.SeamDataKeys;
import com.intellij.seam.facet.SeamFacet;
import org.jetbrains.annotations.NotNull;

public class ShowSeamDependenciesGraph extends AnAction {

  @Override
  public void update(@NotNull final AnActionEvent e) {
    final SeamFacet facet = e.getData(SeamDataKeys.SEAM_FACET);

    e.getPresentation().setEnabledAndVisible(facet != null);
    e.getPresentation().setIcon(SeamIcons.Seam);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final SeamFacet facet = e.getData(SeamDataKeys.SEAM_FACET);
    assert facet != null;

    String moduleName = facet.getModule().getName();

    final VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(
      SeamDependenciesVirtualFileSystem.PROTOCOL + "://" + moduleName);
    FileEditorManager.getInstance(facet.getModule().getProject()).openFile(virtualFile, true);
  }
}
