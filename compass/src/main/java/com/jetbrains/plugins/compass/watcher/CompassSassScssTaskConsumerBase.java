package com.jetbrains.plugins.compass.watcher;

import com.intellij.ide.macro.FileDirMacro;
import com.intellij.ide.macro.FileNameMacro;
import com.intellij.ide.macro.FilePathMacro;
import com.intellij.ide.macro.UnixSeparatorsMacro;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.plugins.watcher.config.BackgroundTaskConsumer;
import com.intellij.plugins.watcher.model.TaskOptions;
import com.intellij.psi.PsiBundle;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jetbrains.plugins.compass.CompassSettings;

import java.util.List;

public class CompassSassScssTaskConsumerBase extends BackgroundTaskConsumer {
  private final FileType myFileType;

  public CompassSassScssTaskConsumerBase(FileType type) {
    myFileType = type;
  }

  @Override
  public boolean isAvailable(PsiFile file) {
    return file.getFileType() == myFileType && isCompassSupportEnabled(ModuleUtilCore.findModuleForPsiElement(file));
  }

  private static boolean isCompassSupportEnabled(@Nullable Module module) {
    final CompassSettings compassSettings = getCompassSettings(module);
    return compassSettings != null && compassSettings.isCompassSupportEnabled();
  }

  @Nullable
  private static CompassSettings getCompassSettings(@NotNull Project project) {
    for (Module module: ModuleManager.getInstance(project).getModules()) {
      final CompassSettings moduleSettings = getCompassSettings(module);
      if (moduleSettings != null && moduleSettings.isCompassSupportEnabled()) {
        return moduleSettings;
      }
    }
    return null;
  }

  @Nullable
  private static CompassSettings getCompassSettings(@Nullable Module module) {
    return module != null ? CompassSettings.getInstance(module) : null;
  }

  @NotNull
  @Override
  public TaskOptions getOptionsTemplate() {
    TaskOptions options = new TaskOptions();
    options.setName("Compass " + myFileType.getName());
    options.setDescription("Compiles ." + myFileType.getDefaultExtension() + " files into .css files using compass");
    options.setFileExtension(myFileType.getDefaultExtension());
    options.setScopeName(PsiBundle.message("psi.search.scope.project"));

    options.setArguments("compile path/to/project $" + new FileNameMacro().getName() + "$");
    options.setWorkingDir("$" + new FileDirMacro().getName() + "$");

    return options;
  }

  @Override
  public void additionalConfiguration(@NotNull Project project, @Nullable PsiFile file, @NotNull TaskOptions options) {
    super.additionalConfiguration(project, file, options);

    if (project.isDefault()) {
      return;
    }

    final Module module = file != null ? ModuleUtilCore.findModuleForPsiElement(file) : null;
    CompassSettings compassSettings = module != null ? getCompassSettings(module) : getCompassSettings(project);
    if (compassSettings != null) {
      final String configDir = VfsUtil.getParentDir(FileUtil.toSystemIndependentName(compassSettings.getCompassConfigPath()));
      List<String> arguments = ContainerUtil.newArrayList("compile");
      arguments.add(StringUtil.notNullize(configDir));
      arguments.add("$" + new UnixSeparatorsMacro().getName() + "($" + new FilePathMacro().getName() + "$)$");
      options.setArguments(ParametersListUtil.join(arguments));

      String compassExecutable = compassSettings.getCompassExecutableFilePath();
      options.setProgram(FileUtil.toSystemIndependentName(compassExecutable));
      options.setWorkingDir(FileUtil.toSystemIndependentName(StringUtil.notNullize(configDir)));
      options.setOutput(FileUtil.toSystemIndependentName(StringUtil.notNullize(configDir)));
    }
  }
}
