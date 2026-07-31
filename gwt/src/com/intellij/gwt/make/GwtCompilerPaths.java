/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.gwt.make;

import com.intellij.compiler.server.BuildManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;

import static org.jetbrains.jps.gwt.model.impl.sdk.JpsGwtDependenciesStorage.GWT_DEPENDENCIES_CACHE_FILE_NAME;

@SuppressWarnings({"HardCodedStringLiteral"})
public final class GwtCompilerPaths {
  private GwtCompilerPaths() {
  }

  public static File getTestGenDirectory(@NotNull Module module) {
    return new File(getTestOutputRoot(module), "gen");
  }

  public static File getTestOutputDirectory(@NotNull Module module) {
    return new File(getTestOutputRoot(module), "www");
  }

  private static File getTestOutputRoot(@NotNull Module module) {
    return new File(getOutputRoot(module), "test");
  }

  public static File getOutputRoot(final @NotNull Module module) {
    return getProjectOutputRoot(module.getProject()).resolve(getOutputDirectoryName(module)).toFile();
  }

  public static @NotNull Path getProjectOutputRoot(@NotNull Project project) {
    return ProjectUtil.getProjectCachePath(project, getOutputRoot());
  }

  public static @NotNull Path getOutputRoot() {
    return PathManager.getSystemDir().resolve("gwt");
  }

  private static String getOutputDirectoryName(@NotNull Module module) {
    final String moduleName = module.getName();
    final String modulePath = module.getModuleFilePath();
    return moduleName.replace(' ', '_') + "." + Integer.toHexString(modulePath.hashCode());
  }

  public static Path getCompileReportInfo(@NotNull Project project) {
    return getProjectOutputRoot(project).resolve("compile-reports-v2.xml");
  }

  public static File getGwtSourcePaths(@NotNull Project project) {
    return getProjectOutputRoot(project).resolve("gwt-source-paths.xml").toFile();
  }

  public static Path getGwtDependencies(Project project) {
    return BuildManager.getInstance().getProjectSystemDirectory(project).toPath().resolve(GWT_DEPENDENCIES_CACHE_FILE_NAME);
  }
}
