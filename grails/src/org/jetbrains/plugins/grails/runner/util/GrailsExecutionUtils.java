// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner.util;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.PathUtil;
import com.intellij.util.io.Compressor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.groovy.grails.rt.GrailsRtMarker;

import java.io.File;
import java.io.IOException;

public final class GrailsExecutionUtils {

  public static final @NonNls String GROOVY_PAGE_ADD_LINE_NUMBERS = "GROOVY_PAGE_ADD_LINE_NUMBERS";
  public static final @NonNls String SERVER_RUNNING_BROWSE_TO = "Server running. Browse to ";
  public static final @NonNls String GRAILS_3_SERVER_MESSAGE = "Grails application running at ";
  private static final @NonNls String AGENT_JAR_SUFFIX = "ideaAgentJar.jar";

  public static int getGrailsConsolePrefixLength(@NotNull String line) {
    if (line.startsWith("|")) {
      if (line.startsWith("| ")) {
        return 2;
      }
      return 1;
    }
    return 0;
  }

  //@NotNull
  //public static RunProfileState addBrowserLauncher(@NotNull final RunProfileState state) {
  //  return (executor, runner) -> {
  //    final ExecutionResult executionResult = state.execute(executor, runner);
  //    if (executionResult == null) throw new ExecutionException("Should be not null");
  //    final ProcessHandler handler = executionResult.getProcessHandler();
  //    handler.addProcessListener(getBrowserLaunchListener(handler));
  //    return executionResult;
  //  };
  //}

  public static @NotNull ProcessListener getBrowserLaunchListener(final ProcessHandler handler, @Nullable String launchUrl) {
    return new ProcessListener() {
      @Override
      public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        String s = event.getText().trim();
        int start = getGrailsConsolePrefixLength(s);

        String url = getUrl(s, SERVER_RUNNING_BROWSE_TO, start);
        if (url == null) url = getUrl(s, GRAILS_3_SERVER_MESSAGE, start);
        if (url != null) {
          BrowserUtil.browse(launchUrl == null ? url: launchUrl);
          handler.removeProcessListener(this);
        }
      }

      String getUrl(String s, String prefix, int start) {
        String url = null;
        if (s.startsWith(prefix, start)) {
          url = s.substring(start + prefix.length()).trim();
          final int space = url.indexOf(' ');
          if (space >= 0) {
            url = url.substring(0, space);
          }
        }
        return url;
      }
    };
  }

  public static void addAgentJar(@NotNull JavaParameters params) {
    String listenerPath = PathUtil.getJarPathForClass(GrailsRtMarker.class);

    for (String vmParam : params.getVMParametersList().getList()) {
      if (vmParam.startsWith("-javaagent:") && (vmParam.endsWith(listenerPath) || vmParam.endsWith(AGENT_JAR_SUFFIX))) {
        return;
      }
    }

    params.getVMParametersList().add("-javaagent:" + ensureJar(listenerPath));
  }

  private static String ensureJar(String path) {
    File file = new File(path);

    if (file.isDirectory()) { // Development mode
      try {
        File tempFile = FileUtil.createTempFile("idea", AGENT_JAR_SUFFIX, true);
        try (Compressor zip = new Compressor.Zip(tempFile)) {
          zip.addDirectory(file);
        }
        return tempFile.getAbsolutePath();
      }
      catch (IOException e) {
        throw new RuntimeException("Failed to create template jar", e);
      }
    }
    else {
      return path;
    }
  }

  public static void addCommonJvmOptions(@NotNull JavaParameters params) {
    if (SystemInfo.isWindows) {
      // See http://youtrack.jetbrains.net/issue/IDEA-73228
      // See jline.WindowsTerminal.readCharacter()    library JLine tries to read character using native API.
      if (!params.getVMParametersList().getParametersString().contains("-Djline.WindowsTerminal.directConsole")) {
        params.getVMParametersList().add("-Djline.WindowsTerminal.directConsole=false");
      }
    }

    if (Boolean.getBoolean("idea.grails.disable.class.modification")) {
      params.getVMParametersList().add("-Didea.grails.disable.class.modification=true");
    }
  }
}
