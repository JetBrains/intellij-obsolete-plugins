// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.maven;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.debugger.impl.RemoteConnectionBuilder;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.JavaProgramPatcher;
import com.intellij.execution.ui.RunContentDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.groovy.grails.rt.Agent;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration;
import org.jetbrains.plugins.grails.runner.impl.GrailsCommandLineState;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.jetbrains.plugins.grails.runner.util.GrailsExecutionUtils.addAgentJar;

final class GrailsMavenDebuggerRunner extends GenericDebuggerRunner {
  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    if (profile instanceof GrailsRunConfiguration && DefaultDebugExecutor.EXECUTOR_ID.equals(executorId)) {
      return ((GrailsRunConfiguration)profile).getGrailsApplicationNullable() instanceof GrailsMavenApplication;
    }
    return false;
  }

  @Override
  public @NotNull String getRunnerId() {
    return getClass().getSimpleName();
  }

  @Override
  protected RunContentDescriptor createContentDescriptor(@NotNull RunProfileState state,
                                                         @NotNull ExecutionEnvironment environment) throws ExecutionException {
    if (!(state instanceof GrailsCommandLineState) || !(((GrailsCommandLineState)state).getExecutor() instanceof MavenCommandExecutor)) {
      return super.createContentDescriptor(state, environment);
    }

    File tmpFile;
    try {
      tmpFile = File.createTempFile("grailsStartFlag", "");
    }
    catch (IOException e) {
      throw new ExecutionException(GrailsBundle.message("dialog.message.failed.to.create.temp.file"), e);
    }

    tmpFile.deleteOnExit();

    JavaParameters parameters = ((JavaCommandLine)state).getJavaParameters();

    addAgentJar(parameters);

    // It's needed for maven project, see AbstractGrailsMojo
    GrailsUtils.addSystemPropertyIfNotExists(parameters.getVMParametersList(), "-DforkDebug=true");

    JavaProgramPatcher.runCustomPatchers(parameters, environment.getExecutor(), environment.getRunProfile());
    RemoteConnection connection = new RemoteConnectionBuilder(false, DebuggerSettings.SOCKET_TRANSPORT, "")
      .asyncAgent(true)
      .project(environment.getProject())
      .create(parameters);

    parameters.getVMParametersList().addProperty(Agent.DEBUG_KIND_FILE, tmpFile.getAbsolutePath());

    RemoteConnection c2 = new RemoteConnection(true, "127.0.0.1", DebuggerUtils.getInstance().findAvailableDebugAddress(true), false);

    ServerSocket serverSocket;

    try {
      serverSocket = new ServerSocket(Integer.parseInt(c2.getDebuggerAddress()));
    }
    catch (IOException e) {
      throw new ExecutionException(GrailsBundle.message("dialog.message.failed.to.open.server.socket"), e);
    }

    try {
      RunContentDescriptor descriptor = attachVirtualMachine(state, environment, c2, true);
      try {
        SocketThread socketThread = new SocketThread(serverSocket.accept(),
                                                     tmpFile,
                                                     descriptor.getProcessHandler(),
                                                     Integer.parseInt(connection.getDebuggerAddress()));
        Thread t = new Thread(socketThread, "grails socket thread");
        t.setDaemon(true);
        t.start();
      }
      catch (IOException e) {
        throw new ExecutionException(GrailsBundle.message("dialog.message.failed.to.open.server.socket"), e);
      }

      return descriptor;
    }
    finally {
      try {
        serverSocket.close();
      }
      catch (IOException ignored) {

      }
    }
  }

  private static final class SocketThread implements Runnable {

    private final Socket mySocket;
    private final File myFile;
    private final ProcessHandler myProcessHandler;
    private final int myMavenProcessPort;

    private SocketThread(Socket socket, File file, ProcessHandler processHandler, int mavenProcessPort) {
      mySocket = socket;
      myFile = file;
      myProcessHandler = processHandler;
      myMavenProcessPort = mavenProcessPort;
    }

    @Override
    public void run() {
      try {
        int debugPort;

        while (true) {
          if (myProcessHandler.isProcessTerminated()) {
            return;
          }

          long length = myFile.length();
          if (length != 0) {
            myFile.delete();

            if (length == 1) {
              // Application will execute in started maven process
              debugPort = myMavenProcessPort;
            }
            else {
              // Application will execute in child process
              debugPort = 5005; // port 5005 is hardcoded in
              Thread.sleep(1000); // Wait for child process start.
            }

            break;
          }

          Thread.sleep(300);
        }

        try (Socket debuggedProcessSocket = new Socket("127.0.0.1", debugPort)) {
          Thread t = new Thread(() -> {
            try {
              copyStream(mySocket.getInputStream(), debuggedProcessSocket.getOutputStream());
            }
            catch (IOException ignored) {

            }
          }, "copy stream");
          t.setDaemon(true);
          t.start();

          copyStream(debuggedProcessSocket.getInputStream(), mySocket.getOutputStream());
        }
      }
      catch (IOException | InterruptedException ignored) {

      }
      finally {
        try {
          mySocket.close();
        }
        catch (IOException ignored) {

        }
      }
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
      byte[] buffer = new byte[4 * 1024];

      int length;
      while ((length = in.read(buffer)) != -1) {
        out.write(buffer, 0, length);
      }
    }
  }
}
