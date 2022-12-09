package com.intellij.dmserver.shell;

import com.intellij.dmserver.deploy.jmx.ConnectorExecuteCommand;
import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import org.jetbrains.annotations.NonNls;

import javax.management.InstanceNotFoundException;
import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public final class DmShellToolWindowPanel extends SimpleToolWindowPanel implements Disposable {
  private static final int POLL_MSEC = 2000;

  private final Project myProject;

  private final DMServerInstance myServerInstance;

  public DmShellToolWindowPanel(Project project, DMServerInstance serverInstance) {
    super(false);
    myProject = project;
    myServerInstance = serverInstance;

    createContent();
  }

  public DMServerInstance getServerInstance() {
    return myServerInstance;
  }

  private void createContent() {
    setBorder(new ToolWindowEx.Border(true, false, false, false));
    setContent(createServerConsole(myServerInstance));
  }

  private JComponent createServerConsole(DMServerInstance serverInstance) {
    TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(myProject);
    ConsoleView consoleView = builder.getConsole();
    ProcessHandler processHandler = new ServerHandler(serverInstance);
    consoleView.attachToProcess(processHandler);
    processHandler.startNotify();
    return consoleView.getComponent();
  }

  @Override
  public void dispose() {

  }

  private static class ServerHandler extends ProcessHandler {
    @NonNls
    private final String CRLF = System.getProperty("line.separator");

    private final Object myMutex = new Object();

    private LinkedList<String> myQueuedCommands = null;

    private final OutputStream myOut;

    private final DMServerInstance myServerInstance;

    ServerHandler(DMServerInstance serverInstance) {
      myServerInstance = serverInstance;
      myOut = new ByteArrayOutputStream() {

        @Override
        public void flush() throws IOException {
          super.flush();
          String inputText = toString();
          processCommand(inputText);
          super.reset();
        }
      };
    }

    @Override
    protected void destroyProcessImpl() {

    }

    @Override
    public boolean detachIsDefault() {
      return false;
    }

    @Override
    protected void detachProcessImpl() {

    }

    @Override
    public OutputStream getProcessInput() {
      return myOut;
    }

    private void processCommand(String commandText) {
      String trimmedCommandText = commandText.trim();
      if (trimmedCommandText.isEmpty()) {
        printInvitation();
        return;
      }

      synchronized (myMutex) {
        if (myQueuedCommands != null) {
          myQueuedCommands.addLast(trimmedCommandText);
        }
        else {
          if (!new CommandProcessor(trimmedCommandText, false).doProcessCommand()) {
            notifyTextAvailable(DmServerBundle.message("DmShellToolWindowPanel.ServerHandler.message.not-ready") + CRLF,
                                ProcessOutputTypes.SYSTEM);
            myQueuedCommands = new LinkedList<>();
            myQueuedCommands.add(trimmedCommandText);
            Thread pollingThread = new Thread(new Runnable() {
              @Override
              public void run() {
                synchronized (this) {
                  try {
                    while (true) {
                      wait(POLL_MSEC);
                      String firstCommand;
                      synchronized (myMutex) {
                        firstCommand = myQueuedCommands.get(0);
                      }
                      if (new CommandProcessor(firstCommand, myQueuedCommands.size() > 1).doProcessCommand()) {
                        synchronized (myMutex) {
                          myQueuedCommands.removeFirst();
                        }
                        while (true) {
                          String nextCommand;
                          synchronized (myMutex) {
                            if (myQueuedCommands.isEmpty()) {
                              myQueuedCommands = null;
                              return;
                            }
                            nextCommand = myQueuedCommands.removeFirst();
                          }
                          new CommandProcessor(nextCommand, true).doProcessCommand();
                        }
                      }
                    }
                  }
                  catch (InterruptedException ignored) {
                  }
                  finally {
                    printInvitation();
                  }
                }
              }
            }, "DM polling");
            pollingThread.start();
          }

        }
        printInvitation();
      }
    }

    private void printInvitation() {
      notifyTextAvailable(CRLF + DmServerBundle.message("DmShellToolWindowPanel.ServerHandler.invitation") + " ", ProcessOutputTypes.SYSTEM);
    }

    @Override
    public void startNotify() {
      super.startNotify();
      printInvitation();
    }

    private class CommandProcessor {

      private final String myCommandText;

      private final boolean myQueued;

      CommandProcessor(String commandText, boolean queued) {
        myCommandText = commandText;
        myQueued = queued;
      }

      public boolean doProcessCommand() {
        try {
          ConnectorExecuteCommand jmxCommand = new ConnectorExecuteCommand(myServerInstance, myCommandText);
          String response = jmxCommand.execute();
          printResponse(response, ProcessOutputTypes.STDOUT);
        }
        catch (TimeoutException e) {
          printResponse(DmServerBundle.message("DmShellToolWindowPanel.CommandProcessor.error.timeout"), ProcessOutputTypes.STDERR);
        }
        catch (ExecutionException e) {
          for (Throwable t = e; t.getCause() != null; t = t.getCause()) {
            if (t.getCause() instanceof InstanceNotFoundException) {
              return false;
            }
          }
          printResponse(DmServerBundle.message("DmShellToolWindowPanel.CommandProcessor.error.execution-error"), ProcessOutputTypes.STDERR);
        }
        return true;
      }

      private void printResponse(String text, Key outputType) {
        if (myQueued) {
          printInvitation();
          notifyTextAvailable(myCommandText + CRLF, ProcessOutputTypes.SYSTEM);
        }
        if (text != null) {
          notifyTextAvailable(text, outputType);
        }
      }
    }
  }
}
