package com.intellij.lang.puppet.util;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.settings.PuppetProjectConfiguration;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.FileContentUtilCore;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public final class PuppetConfigurationUtil {
  public static void reparsePuppetFiles(final Project project) {
    Task.Backgroundable task = new Task.Backgroundable(project, PuppetBundle.message("settings.puppet.reparse.title"), false) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        final Collection<VirtualFile> puppetFiles = getPuppetFiles(project, indicator);
        ApplicationManager.getApplication().invokeLater(() -> FileContentUtilCore.reparseFiles(puppetFiles), ModalityState.nonModal());
      }
    };
    ProgressManager.getInstance().run(task);
  }

  public static Collection<VirtualFile> getPuppetFiles(@NotNull Project project, @Nullable ProgressIndicator indicator) {
    final Collection<VirtualFile> result = new ArrayList<>();
    final VirtualFile baseDir = project.getBaseDir();
    if (baseDir != null) {
      FileBasedIndex.getInstance().iterateIndexableFiles(file -> {
        if (FileTypeRegistry.getInstance().isFileOfType(file, PuppetFileType.INSTANCE)) {
          result.add(file);
        }
        return true;
      }, project, indicator);
    }
    return result;
  }

  public static @NotNull PuppetLanguage.Version getPuppetVersion(@NotNull PsiElement element) {
    return getPuppetVersion(element.getProject());
  }

  public static @NotNull PuppetLanguage.Version getPuppetVersion(@NotNull Project project) {
    return PuppetProjectConfiguration.getInstance(project).getLanguageVersion();
  }
}
