// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.BackgroundTaskQueue;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;

@Service(Service.Level.PROJECT)
public final class GrailsBackgroundService {
  private final BackgroundTaskQueue myQueue;

  public GrailsBackgroundService(Project project) {
    myQueue = new BackgroundTaskQueue(project, GrailsBundle.message("task.queue.title.grails.background.tasks"));
  }

  public void run(Task.Backgroundable task) {
    myQueue.run(task);
  }

  public static @NotNull GrailsBackgroundService getInstance(@NotNull Project project) {
    return project.getService(GrailsBackgroundService.class);
  }
}
