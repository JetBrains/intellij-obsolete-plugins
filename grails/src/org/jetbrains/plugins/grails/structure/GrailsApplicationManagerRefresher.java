// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.structure;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettingsListener;
import org.jetbrains.plugins.grails.config.GrailsConstants;
import org.jetbrains.plugins.grails.config.GrailsFramework;

import java.util.Collection;
import java.util.Set;

/**
 * Registers listeners to queue the recalculation of Grails applications in {@link GrailsApplicationManager}.
 */
final class GrailsApplicationManagerRefresher implements StartupActivity.DumbAware {
  @Override
  public void runActivity(@NotNull Project project) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;

    final GrailsApplicationManager manager = GrailsApplicationManager.getInstance(project);
    final MessageBusConnection connection = project.getMessageBus().connect(manager);

    connection.subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        Boolean inProgress = project.getUserData(GrailsFramework.UPDATE_IN_PROGRESS);
        if (inProgress != null && inProgress) return;
        manager.queueUpdate();
      }
    });

    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkVirtualFileListenerAdapter(new VirtualFileListener() {

      final ProjectFileIndex myFileIndex = ProjectFileIndex.getInstance(project);

      boolean shouldClearApplications(@NotNull VirtualFileEvent event) {
        final VirtualFile file = event.getFile();
        if (!myFileIndex.isInContent(file)) return false;

        final String fileName = event.getFileName();
        return file.isDirectory() && fileName.equals(GrailsConstants.APP_DIRECTORY) ||
               !file.isDirectory() && fileName.equals(GrailsConstants.APPLICATION_PROPERTIES);
      }

      @Override
      public void fileCreated(@NotNull VirtualFileEvent event) {
        if (shouldClearApplications(event)) manager.queueUpdate();
      }

      @Override
      public void fileDeleted(@NotNull VirtualFileEvent event) {
        if (shouldClearApplications(event)) manager.queueUpdate();
      }
    }));

    connection.subscribe(GradleSettingsListener.TOPIC, new GradleSettingsListener() {
      @Override
      public void onProjectsLinked(@NotNull Collection<GradleProjectSettings> settings) {
        manager.queueUpdate();
      }

      @Override
      public void onProjectsUnlinked(@NotNull Set<String> linkedProjectPaths) {
        manager.queueUpdate();
      }
    });
  }
}
