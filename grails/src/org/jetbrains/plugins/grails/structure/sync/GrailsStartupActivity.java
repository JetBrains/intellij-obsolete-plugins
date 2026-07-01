// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.structure.sync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.messages.MessageBusConnection;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
public final class GrailsStartupActivity implements ProjectActivity, DumbAware {

  @Override
  public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return null;

    final MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(GrailsApplicationListener.TOPIC, (GrailsApplicationListener) () -> {
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

    return null;
  }
}
