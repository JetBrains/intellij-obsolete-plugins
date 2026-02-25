// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.runner.impl.Grails3TestCommandLineState;
import org.jetbrains.plugins.grails.runner.impl.GrailsCommandLineState;
import org.jetbrains.plugins.grails.runner.impl.GrailsRunAppCommandLineState;
import org.jetbrains.plugins.grails.runner.impl.GrailsTestCommandLineState;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.mvc.ConsoleProcessDescriptor;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

public abstract class GrailsCommandLineExecutor extends GrailsCommandExecutor {

  private static final String RUN_APP_PATTERN = "\\s*(-D[^\\s]+\\s+)*run-app(\\s.*)?";
  private static final String TEST_APP_PATTERN = "\\s*(-D[^\\s]+\\s+)*test-app(\\s.*)?";

  private @Nullable RunProfileState getSpecialState(@NotNull GrailsRunConfiguration configuration,
                                                    @NotNull ExecutionEnvironment environment) throws ExecutionException {
    final String programParameters = configuration.getProgramParameters();
    if (programParameters == null) return null;

    final Version version = configuration.getGrailsApplication().getGrailsVersion();
    if (programParameters.matches(TEST_APP_PATTERN)) {
      if (version.compareTo(Version.GRAILS_3_0) >= 0) {
        return new Grails3TestCommandLineState(environment, configuration, this);
      }
      else if (version.compareTo(Version.GRAILS_1_2) >= 0) {
        return new GrailsTestCommandLineState(environment, configuration, this);
      }
    }
    else if (programParameters.matches(RUN_APP_PATTERN)) {
      return new GrailsRunAppCommandLineState(environment, configuration, this);
    }
    return null;
  }

  @Override
  public @Nullable RunProfileState getState(@NotNull GrailsRunConfiguration configuration,
                                            @NotNull Executor executor,
                                            @NotNull ExecutionEnvironment environment) throws ExecutionException {
    RunProfileState state = getSpecialState(configuration, environment);
    return state == null ? new GrailsCommandLineState(environment, configuration, this) : state;
  }

  @Override
  public @Nullable ConsoleProcessDescriptor execute(@NotNull GrailsApplication application,
                                                    @NotNull MvcCommand command,
                                                    @Nullable Runnable onDone,
                                                    boolean close,
                                                    String... input) throws ExecutionException {
    final Project project = application.getProject();
    return GrailsConsole.getInstance(project).executeProcess(createCommandLine(application, command), onDone, true, close, input);
  }

  public GeneralCommandLine createCommandLine(@NotNull GrailsApplication application,
                                              @NotNull MvcCommand command) throws ExecutionException {
    JavaParameters params = createJavaParameters(application, command);
    params.setUseDynamicClasspath(application.getProject());
    return params.toCommandLine();
  }

  public abstract @NotNull JavaParameters createJavaParameters(@NotNull GrailsApplication grailsApplication,
                                                               @NotNull MvcCommand command) throws ExecutionException;

  public void addListener(@NotNull JavaParameters params, @NotNull String listener) {
    String listeners = params.getVMParametersList().getPropertyValue("grails.build.listeners");

    if (listeners != null) {
      if (listeners.startsWith("\"") && listeners.endsWith("\"")) {
        listeners = "\"" + listeners.substring(1, listeners.length() - 2) + "," + listener + "\"";
      }
      else {
        listeners = "\"" + listeners + "," + listener + "\"";
      }
    }
    else {
      listeners = listener;
    }

    params.getVMParametersList().replaceOrAppend("grails.build.listeners", "-Dgrails.build.listeners=" + listeners);
  }
}
