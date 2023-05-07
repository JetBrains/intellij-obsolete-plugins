package com.intellij.play.console;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.play.PlayIcons;
import com.intellij.play.utils.PlayBundle;
import com.intellij.play.utils.PlayUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class RunPlayConsoleAction extends AnAction implements DumbAware {

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    Project project = event.getProject();
    assert project != null;

    Pair<String, String> paths = detectOrChooseHomePath(project);
    if (paths != null) {
      String home = paths.first;
      String path = paths.second;
      if (!StringUtil.isEmptyOrSpaces(home) && !StringUtil.isEmptyOrSpaces(path)) {
        PlayConsoleRunner runner = new PlayConsoleRunner(project, home, path);
        runner.showConsoleInRunToolwindow();
      }
    }
  }

  @Nullable
  private static Pair<String, String> detectOrChooseHomePath(Project project) {
    PlayConfiguration playConfiguration = PlayConfiguration.getConfiguration();

    PlayConfigurable playConfigurable = new PlayConfigurable(project);

    initConfiguration(project, playConfiguration);

    Boolean showOnRun = playConfiguration.myShowOnRun;
    if (showOnRun == null || showOnRun.booleanValue()) {
      if (!ShowSettingsUtil.getInstance().editConfigurable(project, playConfigurable)) {
        return null;
      }
      if (checkPaths(project)) return Pair.create(playConfiguration.myPlayHome, playConfiguration.myPath);
    }

    while (StringUtil.isEmptyOrSpaces(playConfiguration.myPlayHome) ||
           StringUtil.isEmptyOrSpaces(playConfiguration.myPath) ||
           checkHome(playConfiguration.myPlayHome) != null ||
           checkDir(playConfiguration.myPath) != null) {
      if (!ShowSettingsUtil.getInstance().editConfigurable(project, playConfigurable)) {
        return null;
      }

      if (checkPaths(project)) break;
    }

    return Pair.create(playConfiguration.myPlayHome, playConfiguration.myPath);
  }

  private static void initConfiguration(Project project, PlayConfiguration playConfiguration) {
    if (playConfiguration.myShowOnRun == null) playConfiguration.myShowOnRun = true;
    if (playConfiguration.myPlayHome == null) playConfiguration.myPlayHome = System.getenv("PLAY_HOME");
    if (playConfiguration.myPath == null) playConfiguration.myPath = project.getBasePath();
  }

  private static boolean checkPaths(Project project) {
    PlayConfiguration playConfiguration = PlayConfiguration.getConfiguration();

    String playHome = playConfiguration.myPlayHome;
    String path = playConfiguration.myPath;

    if (StringUtil.isEmptyOrSpaces(playHome)) {
      Messages.showErrorDialog(project, PlayBundle.message("choose.valid.home.message"), PlayBundle.message("choose.home.title"));
      return false;
    }
    else if (StringUtil.isEmptyOrSpaces(path)) {
      Messages.showErrorDialog(project, PlayBundle.message("choose.valid.working.dir"), PlayBundle.message("choose.workdir.title"));
      return false;
    }

    String errorMessage = checkHome(playHome);
    String pathMessage = checkDir(path);

    if (errorMessage == null && pathMessage == null) {
      return true;
    }

    if (errorMessage != null) {
      Messages.showErrorDialog(project, errorMessage, PlayBundle.message("invalid.home.path"));
    }
    else {
      Messages.showErrorDialog(project, pathMessage, PlayBundle.message("invalid.workdir.path"));
    }
    return false;
  }

  private static @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String checkHome(@NlsSafe String homePath) {
    String message = checkDir(homePath);
    if (message != null) return message;

    String exePath = PlayConsoleRunner.getExePath(homePath);
    if (!new File(exePath).exists()) {
      return PlayBundle.message("play.path.does.not.exist", exePath);
    }
    return null;
  }

  private static @Nullable @Nls(capitalization = Nls.Capitalization.Sentence) String checkDir(String homePath) {
    File file = new File(homePath);
    if (!file.exists()) {
      return PlayBundle.message("play.path.does.not.exist", homePath);
    }
    else if (!file.isDirectory()) {
      return PlayBundle.message("play.path.is.not.directory", homePath);
    }
    return null;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    e.getPresentation().setVisible(project != null && PlayUtils.isPlayInstalled(project));
    e.getPresentation().setIcon(PlayIcons.Play);
  }
}
