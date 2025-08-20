/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.play.console;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.console.LanguageConsoleView;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.*;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class BasicConsoleProcessRunner {
  private OSProcessHandler myProcessHandler;

  public abstract String getHistoryId();

  public abstract String getHistoryPersistenceId();

  public enum State {
    STARTING,
    READY,
    COMPLETING,
    GETTING_DOCUMENTATION,
    EXECUTING
  }

  private volatile State myState = State.STARTING;

  private final LanguageConsoleView myConsoleView;
  private Executor myExecutor;
  private RunContentDescriptor myCurrentContentDescriptor;

  public BasicConsoleProcessRunner(LanguageConsoleView consoleView) {
    myConsoleView = consoleView;
  }

  public boolean runProcess(@NotNull String command) throws ExecutionException {
    myState = State.EXECUTING;

    myProcessHandler = createProcessHandler(createCommandLine(command));

    ProcessTerminatedListener.attach(myProcessHandler);

    getConsoleView().attachToProcess(myProcessHandler);

    final RunContentDescriptor contentDescriptor = getContentDescriptor();
    contentDescriptor.setProcessHandler(myProcessHandler);

    myProcessHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        myState = State.READY;
      }
    });

    RunContentManager.getInstance(getConsoleView().getProject()).showRunContent(getExecutor(), contentDescriptor);

    myProcessHandler.startNotify();

    return true;
  }

  protected RunContentDescriptor createContentDescriptor() {
    return new RunContentDescriptor(getConsoleView(), myProcessHandler, getConsoleComponent(), getConsoleView().getTitle()){
      @Override
      public boolean isContentReuseProhibited() {
        return true;
      }
    };
  }

  protected JComponent getConsoleComponent() {
    return getConsoleView().getComponent();
  }

  @NotNull
  protected Executor createExecutor() {
    return DefaultRunExecutor.getRunExecutorInstance();
  }

  @NotNull
  protected abstract GeneralCommandLine createCommandLine(@NotNull String command);

  public LanguageConsoleView getConsoleView() {
    return myConsoleView;
  }

  protected @NotNull OSProcessHandler createProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
    return new KillableColoredProcessHandler(commandLine);
  }

  public Executor getExecutor() {
    if (myExecutor == null) {
      myExecutor = createExecutor();
    }
    return myExecutor;
  }

  @NotNull
  public RunContentDescriptor getContentDescriptor() {
    if (myCurrentContentDescriptor == null) {
       myCurrentContentDescriptor = createContentDescriptor();
    }
    return myCurrentContentDescriptor;
  }

  public State getState() {
    return myState;
  }

  public boolean isReady() {
    return getState() != State.EXECUTING;
  }

  public OSProcessHandler getProcessHandler() {
    return myProcessHandler;
  }
}
