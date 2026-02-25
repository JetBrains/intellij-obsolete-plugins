// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.groovy.mvc;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.WatchedRootsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class MvcWatchedRootProvider implements WatchedRootsProvider {
  @Override
  public @NotNull Set<String> getRootsToWatch(@NotNull Project project) {
    return doGetRootsToWatch(project);
  }

  public static @NotNull Set<String> doGetRootsToWatch(@NotNull Project project) {
    if (!project.isInitialized()) {
      return Collections.emptySet();
    }

    Set<String> result = null;

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      GrailsFramework framework = GrailsFramework.getInstance(module);
      if (framework == null) {
        continue;
      }

      if (result == null) {
        result = new HashSet<>();
      }

      File sdkWorkDir = framework.getCommonPluginsDir(module);
      if (sdkWorkDir != null) {
        result.add(sdkWorkDir.getAbsolutePath());
      }

      File globalPluginsDir = framework.getGlobalPluginsDir(module);
      if (globalPluginsDir != null) {
        result.add(globalPluginsDir.getAbsolutePath());
      }
    }

    return result == null ? Collections.emptySet() : result;
  }
}
