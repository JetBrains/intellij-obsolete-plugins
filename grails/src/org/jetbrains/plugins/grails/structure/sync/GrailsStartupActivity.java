// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.structure.sync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.projectView.ShowHideKt;
import org.jetbrains.plugins.grails.references.TraitInjectorService;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutor;
import org.jetbrains.plugins.grails.service.GrailsBackgroundService;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationListener;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;

/**
 * Registers listeners for Grails structure events.
 * The purpose is to ensure that
 * <ul>
 * <li>run configuration is created</li>
 * <li>IDEA is able to run Grails commands (i.e {@link GrailsCommandExecutor#getGrailsExecutor(GrailsApplication)} returns non-null value)</li>
 * </ul>
 *
 * @see GrailsApplicationListener
 */
public final class GrailsStartupActivity implements StartupActivity.DumbAware {

  @Override
  public void runActivity(@NotNull Project project) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;

    final MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(GrailsApplicationListener.TOPIC, () -> {
      final GrailsBackgroundService backgroundService = GrailsBackgroundService.getInstance(project);
      if (Registry.is("grails.create.run.configurations")) {
        backgroundService.run(new GrailsRunConfigurationTask(project));
      }
      backgroundService.run(new GrailsSdkCheckTask(project));
      ApplicationManager.getApplication().invokeLater(() -> ShowHideKt.showHide(project), project.getDisposed());
    });

    connection.subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        TraitInjectorService.queueUpdate(project);
      }
    });

    GrailsApplicationManager.getInstance(project).queueUpdate();
    TraitInjectorService.queueUpdate(project);
  }
}
