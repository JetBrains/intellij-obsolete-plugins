package com.intellij.seam.el;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataCache;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.search.CustomPropertyScopeProvider;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.seam.facet.SeamFacet;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.SeamJamModel;
import com.intellij.seam.utils.SeamCommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class SeamModelPropertyScopeProvider implements CustomPropertyScopeProvider {
  private static final Key<CachedValue<Set<VirtualFile>>> CACHED_FILES_KEY = Key.create("cached files");

  private static final UserDataCache<CachedValue<Set<VirtualFile>>, Project, Object> myCachedJavaSeamFiles =
    new UserDataCache<>() {
      @Override
      protected CachedValue<Set<VirtualFile>> compute(final Project project, final Object p) {
        return CachedValuesManager.getManager(project).createCachedValue(
          () -> CachedValueProvider.Result.createSingleDependency(getSeamJamComponentFiles(project),
                                                                  PsiModificationTracker.MODIFICATION_COUNT), false);
      }
    };

  @NotNull
  @Override
  public SearchScope getScope(@NotNull final Project project) {
    final Set<VirtualFile> files = myCachedJavaSeamFiles.get(CACHED_FILES_KEY, project, null).getValue();

    return new GlobalSearchScope(project) {

      @Override
      public boolean contains(@NotNull final VirtualFile file) {
        return files != null && files.contains(file);
      }

      @Override
      public boolean isSearchInModuleContent(@NotNull final Module aModule) {
        return false;
      }

      @Override
      public boolean isSearchInLibraries() {
        return false;
      }
    };
  }

  private static Set<VirtualFile> getSeamJamComponentFiles(final Project project) {
    final Set<VirtualFile> files = new HashSet<>();

    ReadAction.run(() -> {
      for (SeamFacet facet : SeamCommonUtils.getAllSeamFacets(project)) {
        for (SeamJamComponent component : SeamJamModel.getModel(facet.getModule()).getSeamComponents()) {
          PsiFile containingFile = component.getPsiElement().getContainingFile();
          if (containingFile != null) {
            files.add(containingFile.getVirtualFile());
          }
        }
      }
    });
    return files;
  }
}
