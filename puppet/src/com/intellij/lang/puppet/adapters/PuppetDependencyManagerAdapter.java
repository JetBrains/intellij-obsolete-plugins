package com.intellij.lang.puppet.adapters;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.project.PuppetEntity;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base class for puppet dependency managers api
 */
public abstract class PuppetDependencyManagerAdapter {
  private static final ExtensionPointName<PuppetDependencyManagerAdapter> EP_NAME =
    ExtensionPointName.create("com.intellij.puppet.dependencyManagerAdapter");

  /**
   * Checks if adapter may be used to install dependencies for provided puppet entity
   *
   * @return true if we extension may be used to manage dependencies
   */
  public abstract boolean isApplicable(@NotNull PuppetEntity entity);

  /**
   * Dependency installer. Invoked in background task without a ReadAction
   *
   * @param puppetEntity entity to install dependencies for
   * @param indicator    progress indicator of the background task
   */
  protected abstract void installDependencies(@NotNull PuppetEntity puppetEntity, @NotNull ProgressIndicator indicator);

  /**
   * Attempts to install dependencies for puppet entity
   *
   * @param puppetEntity entity to install for
   */
  public final void installDependencies(@Nullable PuppetEntity puppetEntity) {
    if (puppetEntity == null) {
      return;
    }
    final Project project = puppetEntity.getProject();
    new Task.Backgroundable(
      project,
      PuppetBundle.message(
        "puppet.installing.dependencies",
        puppetEntity.getDescriptiveName(),
        puppetEntity.getName()),
      true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        installDependencies(puppetEntity, indicator);
      }
    }.queue();
  }

  /**
   * Do post-installation processing of entity root, should be invoked from adapter implementation after installation is complete
   *
   * @param puppetEntity       entity we've installed dependencies for
   * @param additionalRunnable optional additional activity to do after vfs refresh
   */
  @RequiresReadLock
  protected void doPostInstallationWork(@NotNull PuppetEntity puppetEntity, @Nullable Runnable additionalRunnable) {
    Project project = puppetEntity.getProject();
    if (project.isDisposed()) {
      return;
    }
    VirtualFile entityRoot = puppetEntity.getRoot();
    if (!entityRoot.isValid()) {
      return;
    }

    entityRoot.refresh(true, true);
  }

  public static @NotNull List<PuppetDependencyManagerAdapter> getExtensions() {
    return EP_NAME.getExtensionList();
  }
}
