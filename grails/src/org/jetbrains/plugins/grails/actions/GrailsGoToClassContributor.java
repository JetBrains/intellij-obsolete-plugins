// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.ide.util.gotoByName.DefaultClassNavigationContributor;
import com.intellij.lang.Language;
import com.intellij.navigation.GotoClassContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.util.GroovyUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class GrailsGoToClassContributor implements GotoClassContributor {

  @Override
  public String @NotNull [] getNames(Project project, boolean includeNonProjectItems) {
    Map<String, PsiClass> classes = getGrailsSpecifiedClasses(project);
    if (classes.isEmpty()) return ArrayUtilRt.EMPTY_STRING_ARRAY;

    Set<String> keys = classes.keySet();

    return ArrayUtilRt.toStringArray(keys);
  }

  @Override
  public NavigationItem @NotNull [] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
    Map<String, PsiClass> classes = getGrailsSpecifiedClasses(project);

    PsiClass res = classes.get(name);

    return res == null ? NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY : new NavigationItem[]{res};
  }

  private static Map<String, PsiClass> getGrailsSpecifiedClasses(final Project project) {
    return CachedValuesManager.getManager(project).getCachedValue(project, () -> {
      final Map<String, PsiClass> res = new HashMap<>();

      ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
      final PsiManager psiManager = PsiManager.getInstance(project);

      for (VirtualFile root : rootManager.getContentRoots()) {
        VirtualFile grailsApp = root.findChild(GrailsUtils.GRAILS_APP_DIRECTORY);
        if (grailsApp == null) continue;

        for (VirtualFile file : root.getChildren()) {
          if (file.getName().endsWith("GrailsPlugin.groovy")) {
            PsiFile psiFile = psiManager.findFile(file);
            if (psiFile instanceof GroovyFile) {
              String className = file.getNameWithoutExtension();
              GrTypeDefinition pluginClassDefinition = GroovyUtils.getClassDefinition((GroovyFile)psiFile, className);
              if (pluginClassDefinition != null) {
                res.put(className, pluginClassDefinition);
                break;
              }
            }
          }
        }

        VirtualFile confDir = grailsApp.findChild("conf");
        if (confDir != null) {
          final ProjectFileIndex index = rootManager.getFileIndex();

          if (!index.isInSource(confDir)) {
            VfsUtilCore.visitChildrenRecursively(confDir, new VirtualFileVisitor<Void>() {
              @Override
              public boolean visitFile(@NotNull VirtualFile file) {
                if (file.isDirectory()) {
                  return !index.isInSource(file);
                }

                if (file.getName().endsWith(".groovy")) {
                  PsiFile psiFile = psiManager.findFile(file);
                  if (psiFile != null) {
                    String className = file.getNameWithoutExtension();
                    GrTypeDefinition pluginClassDefinition = GroovyUtils.getClassDefinition((GroovyFile)psiFile, className);
                    if (pluginClassDefinition != null) {
                      res.put(className, pluginClassDefinition);
                    }
                  }
                }

                return true;
              }
            });
          }
        }
      }

      return CachedValueProvider.Result.create(res, PsiModificationTracker.MODIFICATION_COUNT);
    });
  }

  @Override
  public @Nullable String getQualifiedName(@NotNull NavigationItem item) {
    return item instanceof PsiClass ? DefaultClassNavigationContributor.getQualifiedNameForClass((PsiClass)item) : null;
  }

  @Override
  public @Nullable String getQualifiedNameSeparator() {
    return ".";
  }

  @Override
  public @Nullable Language getElementLanguage() {
    return GroovyLanguage.INSTANCE;
  }
}
