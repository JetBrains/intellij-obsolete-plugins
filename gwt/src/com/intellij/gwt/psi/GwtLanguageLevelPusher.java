package com.intellij.gwt.psi;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetConfiguration;
import com.intellij.openapi.module.LanguageLevelUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.JavaLanguageLevelPusherCustomizer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GwtLanguageLevelPusher implements JavaLanguageLevelPusherCustomizer {
  public static void updateConfigurationAndPush(Project project, boolean forcePush) {
    GwtSourcePathsRefresher.getInstance(project).enqueueRefreshSourcePathsTask(forcePush);
  }

  @Override
  public LanguageLevel getImmediateValue(@NotNull Project project, @Nullable VirtualFile file) {
    if (file != null) {
      Module module = ModuleUtilCore.findModuleForFile(file, project);
      if (module != null) {
        GwtFacet gwtFacet = GwtFacet.getInstance(module);
        if (gwtFacet != null) {
          GwtFacetConfiguration configuration = gwtFacet.getConfiguration();

          LanguageLevel moduleLanguageLevel = LanguageLevelUtil.getEffectiveLanguageLevel(module);
          LanguageLevel clientLanguageLevel = configuration.getClientLanguageLevel();

          if (moduleLanguageLevel != clientLanguageLevel) {
            GwtSourcePathsRefresher component = GwtSourcePathsRefresher.getInstance(project);
            if (component.isSourceFile(file) || component.isSuperSourceFile(file)) {
              return clientLanguageLevel;
            }
          }
        }
      }
    }
    return null;
  }

  @Override
  public @Nullable String getInconsistencyLanguageLevelMessage(@NotNull String message,
                                                               @NotNull LanguageLevel level,
                                                               @NotNull PsiFile file) {
    Project project = file.getProject();
    VirtualFile virtualFile = file.getVirtualFile();
    if (GwtFacet.isInModuleWithGwtFacet(project, virtualFile)) {
      GwtSourcePathsRefresher sourcePathsComponent = GwtSourcePathsRefresher.getInstance(project);
      if (sourcePathsComponent.isSourceFile(virtualFile) || sourcePathsComponent.isSuperSourceFile(virtualFile)) {
        return GwtBundle.message("gwt.insufficient.language.level", message);
      }
    }
    return null;
  }
}
