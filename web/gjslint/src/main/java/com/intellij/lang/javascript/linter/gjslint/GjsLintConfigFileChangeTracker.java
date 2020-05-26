package com.intellij.lang.javascript.linter.gjslint;

import com.intellij.lang.javascript.linter.ExtendedLinterState;
import com.intellij.lang.javascript.linter.JSLinterConfigChangeTracker;
import com.intellij.lang.javascript.linter.JSLinterConfigFileUtil;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.text.StringTokenizer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class GjsLintConfigFileChangeTracker extends JSLinterConfigChangeTracker {

  private static final Key<Boolean> PASS_REAL_PATH_KEY = Key.create("PASS_REAL_PATH");

  public GjsLintConfigFileChangeTracker(@NotNull Project project) {
    super(project, Conditions.alwaysTrue());
  }

  public static @NotNull GjsLintConfigFileChangeTracker getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, GjsLintConfigFileChangeTracker.class);
  }

  @Override
  protected boolean isAnalyzerRestartNeeded(@NotNull Project project, @NotNull VirtualFile changedFile) {
    GjsLintConfiguration configuration = GjsLintConfiguration.getInstance(project);
    ExtendedLinterState<GjsLintState> extendedState = configuration.getExtendedState();
    if (extendedState.isEnabled()) {
      String configFilePath = extendedState.getState().getConfigFilePath();
      VirtualFile configVirtualFile = JSLinterConfigFileUtil.findLocalFileByPath(configFilePath);
      if (changedFile.equals(configVirtualFile)) {
        PASS_REAL_PATH_KEY.set(configVirtualFile, null);
        return true;
      }
    }
    return false;
  }

  public static boolean checkPassRealPath(@NotNull VirtualFile configFile) {
    Boolean passRealPath = PASS_REAL_PATH_KEY.get(configFile);
    if (passRealPath == null) {
      passRealPath = isPassRealPath(configFile);
      PASS_REAL_PATH_KEY.set(configFile, passRealPath);
    }
    return passRealPath;
  }

  private static boolean isPassRealPath(@NotNull VirtualFile configFile) {
    String text;
    try {
      text = JSLinterConfigFileUtil.loadActualText(configFile);
    }
    catch (IOException e) {
      return false;
    }
    StringTokenizer st = new StringTokenizer(text, "\n");
    while (st.hasMoreTokens()) {
      String line = st.nextToken();
      line = line.trim();
      if ("--passRealFilePath".equals(line)) {
        return true;
      }
    }
    return false;
  }
}
