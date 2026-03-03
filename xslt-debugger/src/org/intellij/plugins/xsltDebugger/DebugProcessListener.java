// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.intellij.plugins.xsltDebugger;

import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.intellij.plugins.xsltDebugger.rt.engine.DebuggerStoppedException;
import org.jetbrains.annotations.NotNull;

/**
 * ProcessListener that manages the connection to the debugged XSLT-process
 */
class DebugProcessListener extends ProcessAdapter {
  private final Project myProject;
  private final int myPort;
  private final String myAccessToken;

  DebugProcessListener(Project project, int port, String accessToken) {
    myProject = project;
    myPort = port;
    myAccessToken = accessToken;
  }

  @Override
  public void startNotified(@NotNull ProcessEvent event) {
    final DebuggerConnector connector = new DebuggerConnector(myProject, event.getProcessHandler(), myPort, myAccessToken);
    ApplicationManager.getApplication().executeOnPooledThread(connector);
  }

  @Override
  public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
    try {
      final XsltDebuggerSession session = XsltDebuggerSession.getInstance(event.getProcessHandler());
      if (session != null) {
        session.stop();
      }
    } catch (VMPausedException e) {
      // VM is paused, no way for a "clean" shutdown
    } catch (DebuggerStoppedException e) {
      // OK
    }

    super.processWillTerminate(event, willBeDestroyed);
  }

  @Override
  public void processTerminated(@NotNull ProcessEvent event) {
    super.processTerminated(event);

    final XsltDebuggerSession session = XsltDebuggerSession.getInstance(event.getProcessHandler());
    if (session != null) {
      session.close();
    }
  }
}
