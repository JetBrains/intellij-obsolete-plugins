// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureSynchronizer;
import org.jetbrains.plugins.groovy.mvc.plugins.MvcPluginUtil;

import java.util.HashMap;
import java.util.Map;

public final class GrailsStructure {

  private final Module myModule;

  private final VirtualFile myAppRoot;
  private final VirtualFile myAppDirectory;

  private final String myGrailsVersion;

  private final PsiManager myManager;

  private GrailsPropertiesFileCache myGrailsPropertiesFileCache;

  private volatile Map<String, VirtualFile> myInstalledPlugins;

  private GrailsStructure(Module module, @NotNull VirtualFile appDirectory, @Nullable String grailsVersion) {
    myModule = module;
    myManager = PsiManager.getInstance(module.getProject());
    myAppDirectory = appDirectory;
    myAppRoot = myAppDirectory.getParent();
    myGrailsVersion = grailsVersion;
  }

  public @NotNull Module getModule() {
    return myModule;
  }

  public PsiManager getManager() {
    return myManager;
  }

  public Map<String, VirtualFile> getInstalledCommonPlugins() {
    Map<String, VirtualFile> res = myInstalledPlugins;
    if (res == null) {
      res = new HashMap<>();

      if (ApplicationManager.getApplication().isUnitTestMode()) {
        VirtualFile applicationProperties = myAppRoot.findChild("application.properties");
        if (applicationProperties != null) {
          PropertiesFile file = (PropertiesFile)myManager.findFile(applicationProperties);
          assert file != null;
          for (Map.Entry<String, String> entry : MvcPluginUtil.getInstalledPluginVersions(file).entrySet()) {
            res.put(entry.getKey(), null);
          }
        }
      }
      else {
        GrailsFramework.getInstance().collectCommonPluginRoots(res, myModule, false);
      }

      myInstalledPlugins = res;
    }

    return res;
  }

  public boolean isPluginInstalled(String pluginName) {
    return getInstalledCommonPlugins().containsKey(pluginName);
  }

  public @NotNull VirtualFile getAppRoot() {
    return myAppRoot;
  }

  public @NotNull VirtualFile getAppDirectory() {
    return myAppDirectory;
  }

  public @Nullable("Grails version can be unknown, because it's got from name of grails-core.jar") String getGrailsVersion() {
    return myGrailsVersion;
  }

  public boolean isAtLeastGrails(@NotNull String version) {
    return myGrailsVersion != null && VersionComparatorUtil.compare(myGrailsVersion, version) >= 0;
  }

  public boolean isVersionLessThan(@NotNull String version) {
    return myGrailsVersion != null && VersionComparatorUtil.compare(myGrailsVersion, version) < 0;
  }

  public static boolean isVersionAtLeast(@NotNull String grailsVersion, @Nullable Module module) {
    final GrailsStructure structure = getInstance(module);
    return structure != null && structure.isAtLeastGrails(grailsVersion);
  }

  public static boolean isVersionLessThan(@NotNull String grailsVersion, @NotNull Module module) {
    final GrailsStructure grailsStructure = getInstance(module);
    return grailsStructure != null && grailsStructure.isVersionLessThan(grailsVersion);
  }

  public static boolean isVersionLessThan(@NotNull String grailsVersion, @NotNull PsiElement element) {
    final GrailsStructure instance = getInstance(element);
    return instance != null && instance.isVersionLessThan(grailsVersion);
  }

  public boolean isAtLeastGrails1_4() {
    return isAtLeastGrails("1.4");
  }

  public @NotNull String getAppName() {
    String res = getGrailsPropertiesFileCache().getAppName();
    return res == null ? myAppRoot.getName() : res;
  }

  GrailsPropertiesFileCache getGrailsPropertiesFileCache() {
    GrailsPropertiesFileCache res = myGrailsPropertiesFileCache;
    if (res == null || res.isOutdated()) {
      res = new GrailsPropertiesFileCache(this);
      myGrailsPropertiesFileCache = res;
    }

    return res;
  }

  public static @Nullable GrailsStructure getInstance(@NotNull PsiElement element) {
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) return null;

    return getInstance(module);
  }

  public static boolean isAtLeastGrails1_4(@NotNull PsiElement element) {
    GrailsStructure structure = getInstance(element);
    if (structure == null) {
      return true;
    }

    return structure.isAtLeastGrails1_4();
  }

  public static boolean isAtLeastGrails1_4(@NotNull Module module) {
    GrailsStructure structure = getInstance(module);
    if (structure == null) {
      return true;
    }

    return structure.isAtLeastGrails1_4();
  }

  public static @Nullable GrailsStructure getInstance(final @Nullable Module module) {
    if (module == null) return null;
    final Project project = module.getProject();
    return CachedValuesManager.getManager(project).getCachedValue(module, new CachedValueProvider<>() {
      @Override
      public @Nullable Result<GrailsStructure> compute() {
        return Result.create(
          doCompute(),
          MvcModuleStructureSynchronizer.getInstance(project).getFileAndRootsModificationTracker(),
          ProjectRootManager.getInstance(project)
        );
      }

      private GrailsStructure doCompute() {
        final VirtualFile appDirectory = GrailsFramework.getInstance().findAppDirectory(module);
        if (appDirectory != null) {
          final String grailsVersion = GrailsConfigUtils.getGrailsVersion(module);
          return new GrailsStructure(module, appDirectory, grailsVersion);
        }
        else {
          return null;
        }
      }
    });
  }
}
