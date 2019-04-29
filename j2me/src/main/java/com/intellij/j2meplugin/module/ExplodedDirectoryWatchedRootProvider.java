/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.module;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.WatchedRootsProvider;
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * @author nik
 */
public class ExplodedDirectoryWatchedRootProvider implements WatchedRootsProvider {
  private final Project myProject;

  public ExplodedDirectoryWatchedRootProvider(Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public Set<String> getRootsToWatch() {
    Set<String> result = new HashSet<>();
    for (Module module : ModuleManager.getInstance(myProject).getModules()) {
      final String url = J2MEModuleExtension.getInstance(module).getExplodedDirectoryUrl();
      if (url != null) {
        result.add(ProjectRootManagerImpl.extractLocalPath(url));
      }
    }
    return result;
  }
}
