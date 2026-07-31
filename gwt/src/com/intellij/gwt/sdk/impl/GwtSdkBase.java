package com.intellij.gwt.sdk.impl;

import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.psi.GwtSourcePathsRefresher;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.superSource.GwtSuperSourceClassCache;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.model.GwtDependenciesResolver;
import org.jetbrains.jps.gwt.model.GwtSdkPaths;
import org.jetbrains.jps.gwt.model.impl.GwtSourcePath;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.intellij.util.containers.ContainerUtil.getFirstItem;

public abstract class GwtSdkBase implements GwtSdk {
  private static final Logger LOG = Logger.getInstance(GwtSdkBase.class);
  private final Map<String, Boolean> myCachedJreEmulationClasses = new ConcurrentHashMap<>();
  protected @Nullable GwtVersion myVersion;
  protected final GwtSdkPaths myPaths;

  protected GwtSdkBase(GwtSdkPaths paths) {
    myPaths = paths;
  }

  @Override
  public boolean containsJreEmulationClass(List<GwtModule> gwtModules, String className) {
    boolean contains = myCachedJreEmulationClasses.computeIfAbsent(className, cn -> {
      VirtualFile userJar = getUserJar();
      return userJar != null && userJar.findFileByRelativePath(GwtSdkUtil.getJreEmulationClassPath(cn)) != null;
    });
    if (contains) {
      return true;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Cannot find JRE emulation class for " + className + "; gwt-user.jar path: " + getUserJarPath() + ", it " +
                (new File(getUserJarPath()).exists() ? "exists" : "doesn't exist") + ", gwt-user file: " + getUserJar());
    }

    if (gwtModules.isEmpty()) {
      return false;
    }

    Project project = gwtModules.get(0).getManager().getProject();
    GwtSuperSourceClassCache sourceClassCache = GwtSuperSourceClassCache.getInstance(project);
    return sourceClassCache.containsJreEmulationClass(gwtModules, className);
  }

  @Override
  public @Nullable PsiClass findJreEmulationClass(List<GwtModule> gwtModules, @NotNull PsiClass originalClass) {
    final String className = originalClass.getQualifiedName();
    if (className == null) return null;

    Project project = originalClass.getProject();
    GwtSuperSourceClassCache sourceClassCache = GwtSuperSourceClassCache.getInstance(project);

    GwtSourcePathsRefresher sourcePathsRefresher = GwtSourcePathsRefresher.getInstance(project);
    for (GwtSourcePath sourcePath : sourcePathsRefresher.getSuperSourcePaths(gwtModules)) {
      VirtualFile superSourceRoot = LocalFileSystem.getInstance().findFileByPath(sourcePath.myFullPath);
      if (superSourceRoot == null) continue;

      PsiClass psiClass = getFirstItem(sourceClassCache.findClassesByQualifiedName(className, superSourceRoot));
      if (psiClass != null) {
        if (sourcePathsRefresher.isIncluded(psiClass.getContainingFile().getVirtualFile().getPath(), sourcePath)) {
          return psiClass;
        }
      }
    }

    final VirtualFile userJar = getUserJar();
    if (userJar == null) return null;

    final VirtualFile root = userJar.findFileByRelativePath(GwtSdkUtil.EMUL_ROOT);
    if (root == null) return null;

    return getFirstItem(sourceClassCache.findClassesByQualifiedName(className, root));
  }

  @Override
  public void clearCaches() {
    myCachedJreEmulationClasses.clear();
  }

  @Override
  public @NotNull GwtVersion getVersion() {
    if (myVersion == null) {
      myVersion = detectVersion();
    }
    return myVersion;
  }

  protected abstract @NotNull GwtVersion detectVersion();

  @Override
  public @Nullable VirtualFile getUserJar() {
    return GwtSdkUtil.findJarFile(getUserJarPath());
  }

  @Override
  public String getHomeDirectoryUrl() {
    return myPaths.getHomeDirectoryUrl();
  }

  @Override
  public String getDevJarPath() {
    return myPaths.getDevJarPath(getVersion().isUseSystemIndependentGwtDevJar());
  }

  @Override
  public String getServletJarPath() {
    return myPaths.getServletJarPath();
  }

  @Override
  public String getUserJarPath() {
    return myPaths.getUserJarPath();
  }

  @Override
  public @NotNull List<String> getGwtUserDependencies(@NotNull GwtDependenciesResolver dependenciesResolver) {
    return myPaths.getGwtUserDependencies(dependenciesResolver);
  }

  @Override
  public String getCodeServerJarPath() {
    return myPaths.getCodeServerJarPath();
  }

  @Override
  public @NotNull List<String> getGwtDevDependencies(@NotNull GwtDependenciesResolver dependenciesResolver) {
    return myPaths.getGwtDevDependencies(dependenciesResolver);
  }
}
