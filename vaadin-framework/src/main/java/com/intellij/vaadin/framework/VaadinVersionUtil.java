package com.intellij.vaadin.framework;

import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VaadinVersionUtil {
  @NotNull
  public static VaadinVersion getVaadinVersion(@NotNull Module module) {
    VaadinVersion version = getVersionOrNull(module);
    return version != null ? version : getDefaultVersion();
  }

  @NotNull
  public static VaadinVersion getDefaultVersion() {
    return VaadinVersionImpl.V7_OR_LATER;
  }

  public static VaadinVersion[] getAllVersions() {
    return VaadinVersionImpl.values();
  }

  @Nullable
  private static VaadinVersion getVersionOrNull(@NotNull final Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
      JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(module.getProject());
      Object[] dependencies = {ProjectRootManager.getInstance(module.getProject())};
      for (VaadinVersion version : getAllVersions()) {
        if (psiFacade.findClass(version.getServletClass(), scope) != null) {
          return CachedValueProvider.Result.create(version, dependencies);
        }
      }
      return CachedValueProvider.Result.create(null, dependencies);
    });
  }

  public static boolean hasVaadinFramework(@NotNull Module module) {
    return getVersionOrNull(module) != null;
  }

  @Nullable
  private static VirtualFile findVaadinJar(@NotNull Module module) {
    VaadinVersion version = getVersionOrNull(module);
    if (version == null) return null;

    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
    PsiClass aClass = JavaPsiFacade.getInstance(module.getProject()).findClass(version.getServletClass(), scope);
    if (aClass == null) return null;

    PsiFile file = aClass.getContainingFile();
    if (file == null) return null;

    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return null;

    ProjectFileIndex index = ProjectRootManager.getInstance(module.getProject()).getFileIndex();
    VirtualFile root = index.getClassRootForFile(virtualFile);
    return VfsUtilCore.getVirtualFileForJar(root);
  }

  @Nullable
  public static String detectVaadinHome(@NotNull Module module) {
    VirtualFile jar = findVaadinJar(module);
    return jar != null ? jar.getParent().getPath() : null;
  }

  @NotNull
  public static GwtVersion getGwtVersion(@Nullable String vaadinVersion) {
    if (vaadinVersion == null) {
      return GwtVersionImpl.VERSION_2_7;
    }
    if (VersionComparatorUtil.compare(vaadinVersion, "7.2.0") < 0) {
      return GwtVersionImpl.VERSION_2_5;
    }
    if (VersionComparatorUtil.compare(vaadinVersion, "7.4.0") < 0) {
      return GwtVersionImpl.VERSION_2_6;
    }
    if (VersionComparatorUtil.compare(vaadinVersion, "8.0.0") < 0) {
      return GwtVersionImpl.VERSION_2_7;
    }
    return GwtVersionImpl.VERSION_2_8;
  }
}
