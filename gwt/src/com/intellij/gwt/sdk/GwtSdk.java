/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

package com.intellij.gwt.sdk;

import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.model.GwtDependenciesResolver;

import java.util.List;

public interface GwtSdk {
  @NotNull
  GwtVersion getVersion();

  String getDevJarPath();

  boolean containsJreEmulationClass(List<GwtModule> gwtModules, String className);

  boolean isValid();

  String getHomeDirectoryUrl();

  @Nullable
  VirtualFile getUserJar();

  String getServletJarPath();

  String getUserJarPath();

  @NotNull
  List<String> getGwtUserDependencies(@NotNull GwtDependenciesResolver dependenciesResolver);

  String getCodeServerJarPath();

  @NotNull
  List<String> getGwtDevDependencies(@NotNull GwtDependenciesResolver dependenciesResolver);

  @Nullable
  PsiClass findJreEmulationClass(List<GwtModule> gwtModules, @NotNull PsiClass originalClass);

  void clearCaches();
}
