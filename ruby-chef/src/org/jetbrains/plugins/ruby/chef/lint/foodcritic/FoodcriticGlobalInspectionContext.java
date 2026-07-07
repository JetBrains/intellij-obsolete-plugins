package org.jetbrains.plugins.ruby.chef.lint.foodcritic;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.GlobalInspectionContext;
import com.intellij.codeInspection.lang.GlobalInspectionContextExtension;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.chef.ChefUtil;
import org.jetbrains.plugins.ruby.ruby.lang.RubyFileType;

import java.util.Collection;
import java.util.List;

public class FoodcriticGlobalInspectionContext implements GlobalInspectionContextExtension<FoodcriticGlobalInspectionContext> {
  static final Key<FoodcriticGlobalInspectionContext> ID = Key.create("FoodcriticGlobalInspectionContext");

  @Override
  public @NotNull Key<FoodcriticGlobalInspectionContext> getID() {
    return ID;
  }

  @Override
  public void cleanup() {

  }

  @Override
  public void performPostRunActivities(@NotNull List inspections, @NotNull GlobalInspectionContext context) {
  }

  @Override
  public void performPreRunActivities(@NotNull List globalTools, @NotNull List localTools, final @NotNull GlobalInspectionContext context) {
    ApplicationEx application = ApplicationManagerEx.getApplicationEx();

    if (application.isSaveAllowed()) {
      application.invokeAndWait(() -> FileDocumentManager.getInstance().saveAllDocuments());
    }

    final Project project = context.getProject();
    final AnalysisScope analysisScope = context.getRefManager().getScope();
    if (analysisScope == null) return;

    final Collection<PsiDirectory> cookbooks =
      ReadAction.compute(() -> {
        final GlobalSearchScope scope = GlobalSearchScope.EMPTY_SCOPE.union(analysisScope.toSearchScope());
        final Collection<VirtualFile> files = FileTypeIndex.getFiles(RubyFileType.RUBY, scope);

        final PsiManager psiManager = PsiManager.getInstance(project);

        return ContainerUtil.map2SetNotNull(files, file -> {
          final PsiFile psiFile = psiManager.findFile(file);
          if (psiFile == null) return null;

          return ChefUtil.getCookbookByFileInside(psiFile);
        });
      });

    for (PsiDirectory cookbook : cookbooks) {
      final FoodcriticCache foodcriticCache = FoodcriticCache.getInstance(project);
      final FoodcriticCookbookCache cookbookCache = foodcriticCache.getOrCreateCookbookCache(cookbook);

      if (cookbookCache.isInvalidated()) {
        FoodcriticCache.collectWarnings(cookbookCache, cookbook);
      }
    }
  }
}
