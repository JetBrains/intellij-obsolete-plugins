// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.structure.sync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.util.messages.MessageBusConnection;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.structure.GrailsApplicationListener;
import org.jetbrains.plugins.grails.structure.GrailsSDKListener;
import org.jetbrains.plugins.grails.structure.impl.Grails2Application;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureSynchronizer;

final class GrailsMvcStructureRefresher implements ProjectActivity {

  @Override
  public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return null;

    final MvcModuleStructureSynchronizer synchronizer = MvcModuleStructureSynchronizer.getInstance(project);
    final MessageBusConnection connection = project.getMessageBus().connect();

    connection.subscribe(GrailsApplicationListener.TOPIC, (GrailsApplicationListener) () -> {
      synchronizer.getFileAndRootsModificationTracker().incModificationCount();
      ApplicationManager.getApplication().invokeLater(() -> {
        synchronizer.queue(MvcModuleStructureSynchronizer.SyncAction.UpdateProjectStructure, project);
        synchronizer.queue(MvcModuleStructureSynchronizer.SyncAction.UpgradeFramework, project);
      }, project.getDisposed());
    });

    connection.subscribe(GrailsSDKListener.TOPIC, (GrailsSDKListener) application -> {
      if (!(application instanceof Grails2Application)) return;
      synchronizer.getFileAndRootsModificationTracker().incModificationCount();
      Module module = ((Grails2Application)application).getModule();
      ApplicationManager.getApplication().invokeLater(
              () -> GrailsFramework.forceSynchronizationSetting(module),
              module.getDisposed()
      );
    });

    return null;
  }
}
