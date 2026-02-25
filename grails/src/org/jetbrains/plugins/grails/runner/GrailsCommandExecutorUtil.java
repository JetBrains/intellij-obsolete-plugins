// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.groovy.mvc.ConsoleProcessDescriptor;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

public final class GrailsCommandExecutorUtil {

  public static @Nullable ConsoleProcessDescriptor execute(@NotNull GrailsApplication application,
                                                           @NotNull MvcCommand command) {

    return execute(application, command, null);
  }

  public static @Nullable ConsoleProcessDescriptor execute(@NotNull GrailsApplication application,
                                                           @NotNull MvcCommand command,
                                                           @Nullable Runnable onDone) {
    return execute(application, command, onDone, true);
  }

  public static @Nullable ConsoleProcessDescriptor execute(@NotNull GrailsApplication application,
                                                           @NotNull MvcCommand command,
                                                           @Nullable Runnable onDone,
                                                           boolean close,
                                                           String... input) {
    try {
      final GrailsCommandExecutor executor = GrailsCommandExecutor.getGrailsExecutor(application);
      if (executor == null) return null;
      return executor.execute(application, command, onDone, close, input);
    }
    catch (ExecutionException e) {
      GrailsConsole.getInstance(application.getProject());
      GrailsConsole.NOTIFICATION_GROUP.createNotification(e.getMessage(), NotificationType.ERROR);
      return null;
    }
  }

  public static void executeInModal(@NotNull GrailsApplication application,
                                    @NotNull MvcCommand command,
                                    @NotNull @NlsContexts.ProgressText String title,
                                    @Nullable Runnable onDone,
                                    boolean close) {
    assert !GrailsConsole.getInstance(application.getProject()).isExecuting();
    final Ref<ProgressIndicator> processIndicatorRef = new Ref<>();
    Runnable run = onDone == null ? null : () -> {
      ProgressIndicator indicator = processIndicatorRef.get();
      if (indicator != null && indicator.isCanceled()) return;

      ApplicationManager.getApplication().invokeLater(onDone);
    };

    try {
      GeneralCommandLine commandLine = createCommandLine(application, command);
      ConsoleProcessDescriptor descriptor = GrailsConsole.executeProcess(application.getProject(), commandLine, run, close);
      ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        processIndicatorRef.set(progressIndicator);

        progressIndicator.setText(title);

        descriptor.addProcessListener(new ProcessListener() {
          @Override
          public void onTextAvailable(final @NotNull ProcessEvent event, final @NotNull Key outputType) {
            progressIndicator.setText2(event.getText());
          }
        }).waitWith(progressIndicator);
      }, title, true, application.getProject());
    }
    catch (ExecutionException e) {
      GrailsConsole.getInstance(application.getProject());
      GrailsConsole.NOTIFICATION_GROUP.createNotification(e.getMessage(), NotificationType.ERROR);
    }
  }

  public static @NotNull GeneralCommandLine createCommandLine(@NotNull GrailsApplication application,
                                                              @NotNull MvcCommand command) throws ExecutionException {
    final GrailsCommandExecutor executor = GrailsCommandExecutor.getGrailsExecutor(application);
    if (executor instanceof GrailsCommandLineExecutor) {
      return ((GrailsCommandLineExecutor)executor).createCommandLine(application, command);
    }
    throw new ExecutionException(GrailsBundle.message("dialog.message.cannot.create.command.line.for", command, application.getRoot()));
  }
}
