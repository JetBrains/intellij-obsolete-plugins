package com.intellij.play.console;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.console.LanguageConsoleImpl;
import com.intellij.execution.console.LanguageConsoleView;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.play.utils.PlayBundle;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class PlayConsoleRunner extends BasicProcessConsoleRunnerWithHistory<LanguageConsoleView> {
  public static final Key<Object> PLAY_CONSOLE_KEY = Key.create(PlayConsoleRunner.class.getSimpleName());

  private final String myPlayHome;
  private final String myWorkingDir;

  public PlayConsoleRunner(@NotNull Project project, @NotNull String playHome, @NotNull String workingDir) {
    super(createConsoleView(project));

    myPlayHome = playHome;
    myWorkingDir = workingDir;
  }

  @NotNull
  private static LanguageConsoleView createConsoleView(Project project) {
    LanguageConsoleView consoleView = new LanguageConsoleImpl(project, PlayBundle.PLAY_FRAMEWORK, PlainTextLanguage.INSTANCE);
    consoleView.getConsoleEditor().getDocument().putUserData(PLAY_CONSOLE_KEY, Boolean.TRUE);
    consoleView.setPrompt("play ");
    return consoleView;
  }

  @Override
  protected GeneralCommandLine createCommandLine(@NotNull String command) {
    return new GeneralCommandLine(getExePath(myPlayHome))
      .withParameters(getParameters(command))
      .withEnvironment("PLAY_OPTS", getOptions())
      .withWorkDirectory(myWorkingDir);
  }

  private static List<String> getParameters(@NotNull String command) {
    List<String> params = new LinkedList<>();
    for (String s : StringUtil.split(command, " ")) {
      if (!"play".equals(s.trim())) {
        params.add(s.trim());
      }
    }
    return params;
  }

  static String getExePath(String playHome) {
    if (!playHome.endsWith(File.separator)) playHome += File.separator;

    return playHome + (SystemInfo.isWindows ? "play.bat" : "play");
  }

  protected String getOptions() {
    //noinspection SpellCheckingInspection
    return "-Djline.WindowsTerminal.directConsole=false";
  }

  @Override
  protected String getToolWindowId() {
    return ToolWindowId.RUN;
  }
}
