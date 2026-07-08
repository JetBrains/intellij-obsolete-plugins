package com.intellij.lang.puppet.adapters;

import com.intellij.ide.projectView.actions.MarkRootsManager;
import com.intellij.lang.puppet.project.PuppetEntity;
import com.intellij.lang.puppet.project.PuppetModule;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class PuppetAbstractLibrarianAdapter extends PuppetDependencyManagerAdapter {
  public static final String LIBRARIAN_EXECUTABLE_NAME = "librarian-puppet";
  public static final String LIBRARIAN_EXECUTABLE_WIN_NAME = LIBRARIAN_EXECUTABLE_NAME + ".bat";

  private static final String LIBRARIAN_INTERNAL_DIR_NAME = ".librarian";

  protected abstract @NotNull Logger getLogger();

  @Override
  public boolean isApplicable(@NotNull PuppetEntity entity) {
    return entity.getPuppetFile() != null || entity instanceof PuppetModule && !((PuppetModule)entity).isHeadless();
  }

  /**
   * Creates a list of commandline arguments for librarian execution
   *
   * @param entity entity we are working with
   * @return list of arguments or null if arguments can't be calculated and librarian should not be executed
   */
  protected @Nullable List<String> createLibrarianArguments(@NotNull PuppetEntity<?> entity) {
    return Arrays.asList("install", "--verbose", "--path=" + entity.getLibrarianDependenciesRootName());
  }

  /**
   * Configures environment for librarian-puppet execution
   *
   */
  protected void configureEnvironment(@NotNull Map<String, String> env) {
    String librarianCachePath = FileUtil.toSystemIndependentName(FileUtil.join(FileUtil.getTempDirectory(), ".librarian"));
    getLogger().info(MessageFormat.format("Using {0} as librarian cache directory", librarianCachePath));
    env.put("LIBRARIAN_PUPPET_TMP", librarianCachePath);
  }

  /**
   * Returns librarian-puppet internal directory, used for some internal files
   *
   * @param root puppet entity root
   * @return internal directory virtual file if exists
   */
  @RequiresReadLock
  protected static @Nullable VirtualFile getLibrarianInternalDir(@NotNull VirtualFile root) {
    if (!root.isValid()) {
      return null;
    }
    return root.findChild(LIBRARIAN_INTERNAL_DIR_NAME);
  }

  /**
   * Marks virtual file as excluded in current project
   *
   * @param project project
   * @param file    file to exclude
   */
  public static void excludeVirtualFile(@NotNull Project project, @NotNull VirtualFile file) {
    Application application = ApplicationManager.getApplication();

    application.invokeLater(() -> {
      if (project.isDisposed() || !file.isValid()) {
        return;
      }

      Module currentModule = ModuleUtilCore.findModuleForFile(file, project);
      if (currentModule == null) {
        return;
      }

      ModifiableRootModel model = ModuleRootManager.getInstance(currentModule).getModifiableModel();
      ContentEntry entry = MarkRootsManager.findContentEntry(model, file);
      if (entry == null) {
        return;
      }

      entry.addExcludeFolder(file);

      application.runWriteAction(() -> model.commit());
    });
  }
}
