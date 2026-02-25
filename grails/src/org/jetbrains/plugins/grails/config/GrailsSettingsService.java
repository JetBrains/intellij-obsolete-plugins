// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.HashMap;
import java.util.Map;

@State(name = "grailsSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class GrailsSettingsService implements PersistentStateComponent<GrailsSettingsService.StateHolder> {
  private static final Key<GrailsSettings> TEST_DATA_KEY = Key.create("TEST_DATA_KEY");

  private final Map<Module, GrailsSettings> map = new HashMap<>();

  private final Project project;

  public GrailsSettingsService(Project project) {
    this.project = project;
  }

  public static @NotNull GrailsSettings getGrailsSettings(@NotNull Module module) {
    GrailsSettingsService service = module.getProject().getService(GrailsSettingsService.class);

    final Map<Module, GrailsSettings> map = service.map;

    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (map) {
      GrailsSettings res = map.get(module);
      if (res == null) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
          VirtualFile conf = GrailsUtils.findConfDirectory(module);
          if (conf != null) {
            VirtualFile buildConfig = conf.findChild(GrailsUtils.BUILD_CONFIG);
            if (buildConfig != null) {
              res = buildConfig.getUserData(TEST_DATA_KEY);
              if (res != null) {
                map.put(module, res);
                return res;
              }
            }
          }
        }

        res = new GrailsSettings();
        map.put(module, res);
      }
      return res;
    }
  }

  @TestOnly
  public static Key<GrailsSettings> getTestDataKey() {
    return TEST_DATA_KEY;
  }

  public static String getGrailsWorkDir(@NotNull Module module) {
    GrailsSettings settings = getGrailsSettings(module);
    String res = settings.properties.get(PrintGrailsSettingsConstants.WORK_DIR);
    if (res != null) {
      return res;
    }

    return SystemProperties.getUserHome() + "/.grails/" + GrailsConfigUtils.getGrailsVersion(module);
  }

  public static @Nullable String getProjectWorkDir(@NotNull Module module) {
    GrailsSettings settings = getGrailsSettings(module);
    String res = settings.properties.get(PrintGrailsSettingsConstants.PROJECT_WORK_DIR);
    if (res != null) {
      return res;
    }

    String appName;

    GrailsStructure structure = GrailsStructure.getInstance(module);
    if (structure == null) {
      VirtualFile root = GrailsFramework.getInstance().findAppRoot(module);
      if (root == null) return null;
      appName = root.getName();
    }
    else {
      if (structure.isAtLeastGrails1_4()) { // #CHECK# BuildSettings.establishProjectStructure()
        appName = structure.getAppName();
      }
      else {
        appName = structure.getAppRoot().getName();
      }
    }

    return getGrailsWorkDir(module) + "/projects/" + appName;
  }

  public static @Nullable String getProjectPluginsDir(@NotNull Module module) {
    GrailsSettings settings = getGrailsSettings(module);
    String res = settings.properties.get(PrintGrailsSettingsConstants.PLUGINS_DIR);
    if (res != null) {
      return res;
    }

    String workDir = getProjectWorkDir(module);
    return workDir == null ? null : workDir + "/plugins";
  }

  public static String getGlobalPluginsDir(@NotNull Module module) {
    GrailsSettings settings = getGrailsSettings(module);
    String res = settings.properties.get(PrintGrailsSettingsConstants.GLOBAL_PLUGINS_DIR);
    if (res != null) {
      return res;
    }

    return getGrailsWorkDir(module) + "/global-plugins";
  }

  public static boolean isDebugRunForked(@NotNull Module module) {
    final String res = getGrailsSettings(module).properties.get(PrintGrailsSettingsConstants.DEBUG_RUN_FORK);
    return Boolean.parseBoolean(res);
  }

  public static boolean isDebugTestForked(@NotNull Module module) {
    final String res = getGrailsSettings(module).properties.get(PrintGrailsSettingsConstants.DEBUG_TEST_FORK);
    return Boolean.parseBoolean(res);
  }

  @Override
  public StateHolder getState() {
    StateHolder res = new StateHolder();

    for (Map.Entry<Module, GrailsSettings> entry : map.entrySet()) {
      Module module = entry.getKey();
      if (!module.isDisposed()) {
        res.map.put(module.getName(), entry.getValue());
      }
    }

    return res;
  }

  @Override
  public void loadState(@NotNull StateHolder state) {
    map.clear();

    ModuleManager moduleManager = ModuleManager.getInstance(project);

    for (Map.Entry<String, GrailsSettings> entry : state.map.entrySet()) {
      Module module = moduleManager.findModuleByName(entry.getKey());
      if (module != null) {
        map.put(module, entry.getValue());
      }
    }
  }

  public static class StateHolder {
    public Map<String, GrailsSettings> map = new HashMap<>();
  }
}
