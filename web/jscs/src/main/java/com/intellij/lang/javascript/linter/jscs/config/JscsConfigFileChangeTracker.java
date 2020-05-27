package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.lang.javascript.linter.JSLinterConfigChangeTracker;
import com.intellij.lang.javascript.linter.JSLinterConfigFileUtil;
import com.intellij.lang.javascript.linter.jscs.JscsConfiguration;
import com.intellij.lang.javascript.linter.jscs.JscsState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author by Irina.Chernushina on 9/29/2014.
 */
public class JscsConfigFileChangeTracker extends JSLinterConfigChangeTracker {
  public JscsConfigFileChangeTracker(@NotNull Project project) {
    super(project, JscsConfigFileType.INSTANCE);
  }

  public static JscsConfigFileChangeTracker getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, JscsConfigFileChangeTracker.class);
  }

  @Override
  protected boolean isAnalyzerRestartNeeded(@NotNull Project project, @NotNull VirtualFile changedFile) {
    final JscsConfiguration configuration = JscsConfiguration.getInstance(project);
    final JscsState state = configuration.getExtendedState().getState();
    if (state.isCustomConfigFileUsed()) {
      final VirtualFile configVirtualFile = JSLinterConfigFileUtil.findLocalFileByPath(state.getCustomConfigFilePath());
      return changedFile.equals(configVirtualFile);
    }
    return true;
  }
}
