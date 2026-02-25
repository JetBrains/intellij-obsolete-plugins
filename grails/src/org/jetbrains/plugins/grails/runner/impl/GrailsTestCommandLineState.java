// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner.impl;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.runner.GrailsCommandLineExecutor;
import org.jetbrains.plugins.grails.runner.GrailsRerunFailedTestsAction;
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration;
import org.jetbrains.plugins.grails.tests.runner.GrailsUrlProvider;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class GrailsTestCommandLineState extends GrailsTestAppCommandLineState {

  public static final String FRAMEWORK_NAME = "GrailsTests";
  public static final String GRAILS_LISTENER_NAME = "org.jetbrains.groovy.grails.rt.GrailsIdeaTestListener";

  public GrailsTestCommandLineState(@NotNull ExecutionEnvironment environment,
                                    @NotNull GrailsRunConfiguration configuration,
                                    @NotNull GrailsCommandLineExecutor executor) throws ExecutionException {
    super(environment, configuration, executor);
  }

  @Override
  protected @NotNull JavaParameters doCreateJavaParameters(@NotNull MvcCommand command) throws ExecutionException {
    final JavaParameters parameters = super.doCreateJavaParameters(command);
    getExecutor().addListener(parameters, GRAILS_LISTENER_NAME);
    return parameters;
  }

  @Override
  public @NotNull ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
    final ProcessHandler processHandler = startProcess();
    final SMTRunnerConsoleProperties properties = new SMTRunnerConsoleProperties(getConfiguration(), FRAMEWORK_NAME, executor) {
      @Override
      public @NotNull SMTestLocator getTestLocator() {
        return GrailsUrlProvider.INSTANCE;
      }
    };
    final SMTRunnerConsoleView consoleView = (SMTRunnerConsoleView)SMTestRunnerConnectionUtil.createAndAttachConsole(
      FRAMEWORK_NAME, processHandler, properties
    );

    // See #IDEA-62538. Grails can ask some question, but it will not be displayed to user because question hasn't  '\n' at end.
    final OutputStream input = processHandler.getProcessInput();
    try (PrintStream ps = new PrintStream(input, false, StandardCharsets.UTF_8)) {
      ps.print("n\nn\nn\nn\nn\nn\nn\nn\nn\nn\n");
      ps.flush();
    }

    final GrailsRerunFailedTestsAction rerunFailedTestsAction = new GrailsRerunFailedTestsAction(consoleView, consoleView.getProperties());
    rerunFailedTestsAction.setModelProvider(consoleView::getResultsViewer);

    DefaultExecutionResult result = new DefaultExecutionResult(consoleView, processHandler, createActions(consoleView, processHandler));
    result.setRestartActions(rerunFailedTestsAction);
    return result;
  }
}
