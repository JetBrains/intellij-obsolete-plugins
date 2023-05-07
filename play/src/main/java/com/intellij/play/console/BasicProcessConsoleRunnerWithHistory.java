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

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.console.LanguageConsoleView;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class BasicProcessConsoleRunnerWithHistory<T extends LanguageConsoleView> {

  private final T myConsoleView;
  private BasicConsoleProcessRunner myProcessRunner;
  private ProcessLanguageConsoleViewWrapper myComponent;

  public BasicProcessConsoleRunnerWithHistory(@NotNull T consoleView) {
    myConsoleView = consoleView;

    getComponent();
  }

  public void showConsoleInRunToolwindow() {
    UIUtil.invokeLaterIfNeeded(() -> showConsoleInnerRunToolwindow());
  }

  protected BasicConsoleProcessRunner createBasicConsoleProcessRunner() {
    return new BasicConsoleProcessRunner(myConsoleView) {
      @NotNull
      @Override
      protected GeneralCommandLine createCommandLine(@NotNull String command) {
        return BasicProcessConsoleRunnerWithHistory.this.createCommandLine(command);
      }

      @Override
      public String getHistoryId() {
        return getToolWindowId();
      }

      @Override
      public String getHistoryPersistenceId() {
        return getToolWindowId();
      }

      @Override
      protected JComponent getConsoleComponent() {
        return getComponent();
      }
    };
  }

  protected abstract GeneralCommandLine createCommandLine(String command);

  public BasicConsoleProcessRunner getProcessRunner() {
    if (myProcessRunner == null) {
      myProcessRunner = createBasicConsoleProcessRunner();
    }
    return myProcessRunner;
  }

  private void showConsoleInnerRunToolwindow() {
    final BasicConsoleProcessRunner processRunner = getProcessRunner();

    Executor executor = processRunner.getExecutor();
    RunContentManager.getInstance(getProject()).showRunContent(executor, processRunner.getContentDescriptor());

    ToolWindowManager.getInstance(getProject()).getToolWindow(executor.getId()).activate(
      () -> IdeFocusManager.getInstance(getProject()).requestFocus(myConsoleView.getCurrentEditor().getContentComponent(), true));
  }

  public Project getProject() {
    return myConsoleView.getProject();
  }

  public ProcessLanguageConsoleViewWrapper getComponent() {
    if (myComponent == null) {
      myComponent = new ProcessLanguageConsoleViewWrapper(myConsoleView) {
        @Override
        protected BasicConsoleProcessRunner getProcessRunner() {
          return BasicProcessConsoleRunnerWithHistory.this.getProcessRunner();
        }
      };
    }
    return myComponent;
  }

  protected abstract String getToolWindowId();
}
