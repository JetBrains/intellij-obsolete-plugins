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

package com.intellij.gwt.module;

import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.css.CssClass;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class GwtModulesManager  {

  public static GwtModulesManager getInstance(@NotNull Project project) {
    return project.getService(GwtModulesManager.class);
  }

  public abstract @NotNull List<GwtModule> getAllGwtModules();

  public abstract @NotNull Collection<VirtualFile> getGwtModuleFiles(@NotNull GlobalSearchScope scope);

  public abstract @NotNull List<GwtModule> getGwtModules(@NotNull Module module, final boolean includeTests);

  public abstract Collection<GwtModule> getGwtModuleToCompile(Module module, boolean includeTests);

  public abstract Collection<GwtModule> getCompilableGwtModules(Module module, boolean includeTests);

  public abstract @NotNull List<GwtModule> findGwtModulesByClientSourceFile(@NotNull VirtualFile file);

  public abstract @Nullable GwtModule findGwtModuleByClientSourceFile(@NotNull VirtualFile file);

  public abstract @Nullable GwtModule findGwtModuleByQualifiedName(final @NotNull String qualifiedName, final GlobalSearchScope scope);
  public abstract List<GwtModule> findGwtModulesByQualifiedName(String qualifiedName, GlobalSearchScope scope);

  public abstract @NotNull Collection<GwtModule> findGwtModulesByOutputName(final @NotNull String outputName, final GlobalSearchScope scope);

  public abstract @NotNull List<GwtModule> findModulesByClass(@NotNull PsiElement context, final @Nullable String className);

  public abstract @Nullable GwtModule findGwtModuleByEntryPoint(@NotNull PsiClass psiClass);

  public abstract @Nullable GwtModule getGwtModule(@NotNull PsiFile gwtXmlFile);

  public abstract @NotNull List<Pair<GwtModule, String>> findGwtModulesByPublicFile(@NotNull  VirtualFile file);

  public abstract @Nullable @NlsSafe String getOutputPath(@NotNull GwtModule gwtModule, @NotNull VirtualFile file);


  public abstract XmlFile @NotNull [] findHtmlFilesByModule(@NotNull GwtModule module);

  public abstract @NotNull Collection<VirtualFile> getAllHtmlFiles(GwtModule module);

  public abstract @Nullable PsiElement findTagById(@NotNull XmlFile htmlFile, String id);


  public abstract String[] getAllIds(@NotNull XmlFile htmlFile);

  public abstract @NotNull Set<CssClass> findCssClasses(GwtModule module, String className);

  public abstract String[] getAllCssClassNames(final GwtModule module);

  public abstract @Nullable StylesheetFile findPreferableCssFile(final GwtModule module);

  public abstract boolean isInheritedOrSelf(GwtModule gwtModule, GwtModule inheritedModule);

  public abstract boolean isInheritedOrSelf(GwtModule gwtModule, List<GwtModule> referencedModules);

  public abstract @NotNull Collection<GwtModule> findAllInheritedModules(@NotNull Collection<GwtModule> modules, @NotNull GlobalSearchScope scope);

  public abstract boolean isLibraryModule(GwtModule module);

  public abstract boolean isUnderGwtModule(final VirtualFile virtualFile);

  public abstract @Nullable GwtModule getGwtModuleByXmlFile(@NotNull PsiFile file);


}
