package org.jetbrains.plugins.ruby.chef.lint.foodcritic;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.intention.EmptyIntentionAction;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.chef.ChefUtil;
import org.jetbrains.plugins.ruby.gem.util.RubyGemSearchUtil;
import org.jetbrains.plugins.ruby.ruby.RModuleUtil;
import org.jetbrains.plugins.ruby.ruby.lang.RubyFileType;

public final class FoodcriticAnnotator extends ExternalAnnotator<FoodcriticState, FoodcriticResult> {
  static final Logger LOG = Logger.getInstance(FoodcriticAnnotator.class);

  @Override
  public @Nullable FoodcriticState collectInformation(final @NotNull PsiFile psiFile) {
    VirtualFile vFile = psiFile.getVirtualFile();
    if (vFile == null || !FileTypeRegistry.getInstance().isFileOfType(vFile, RubyFileType.RUBY)) return null;

    final Module module = ModuleUtilCore.findModuleForPsiElement(psiFile);
    if (module == null) return null;

    final Sdk sdk = RModuleUtil.getInstance().findRubySdkForModule(module);
    if (sdk == null) return null;

    if (RubyGemSearchUtil.findGem(module, sdk, FoodcriticCache.FOODCRITIC_GEM_NAME) == null) return null;

    final PsiDirectory cookbook = ChefUtil.getCookbookByFileInside(psiFile);
    if (cookbook == null) return null;

    return new FoodcriticState(cookbook, psiFile);
  }

  @Override
  public @Nullable FoodcriticResult doAnnotate(final @NotNull FoodcriticState state) {
    final FoodcriticCache foodcriticCache = FoodcriticCache.getInstance(state.file.getProject());
    final PsiDirectory cookbook = state.cookbook;
    final FoodcriticCookbookCache cookbookCache = foodcriticCache.getOrCreateCookbookCache(cookbook);

    if (cookbookCache.isInvalidated()) {
      FoodcriticCache.collectWarnings(cookbookCache, cookbook);
    }

    return cookbookCache.getCachedProblem(state);
  }

  @Override
  public void apply(final @NotNull PsiFile psiFile,
                    final @Nullable FoodcriticResult annotationResult,
                    final @NotNull AnnotationHolder holder) {
    final Project project = psiFile.getProject();
    if (annotationResult == null) return;

    if (!psiFile.isValid()) {
      LOG.info("File is invalid, skip.");
      return;
    }

    final Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
    if (document == null) return;

    final String path = psiFile.getVirtualFile().getPath();
    for (FoodcriticProblem problem : annotationResult.getProblems()) {
      if (!path.endsWith(problem.path)) return;

      final int line = problem.lineNumber;

      if (line < 1 || line > document.getLineCount()) return;
      int startOffset = document.getLineStartOffset(line - 1);
      final int endOffset = document.getLineEndOffset(line - 1);
      final String lineText = document.getText(new TextRange(startOffset, endOffset));

      final int trimLeading = lineText.length() - StringUtil.trimLeading(lineText).length();
      final int trimTrailing = lineText.length() - StringUtil.trimTrailing(lineText).length();

      final TextRange textRange = new TextRange(startOffset + trimLeading, endOffset - trimTrailing);
      final HighlightDisplayKey key = HighlightDisplayKey.find(FoodcriticInspection.FOODCRITIC_INSPECTION_SHORT_NAME);
      final EmptyIntentionAction emptyIntentionAction = new EmptyIntentionAction(FoodcriticInspection.getFoodcriticInspectionDisplayName());

      @NlsSafe String description = problem.description;
      holder.newAnnotation(HighlightSeverity.WARNING, description)
        .range(textRange)
        .newFix(emptyIntentionAction).range(textRange).key(key).registerFix()
        .create();
    }
  }
}