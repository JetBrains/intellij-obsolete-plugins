package com.intellij.lang.puppet.ide.highlighting;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase;
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerFactoryBase;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.psi.references.PuppetPolyVariantCachingReferenceBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetHighlightUsagesHandlerFactory extends HighlightUsagesHandlerFactoryBase {
  @Override
  public @Nullable HighlightUsagesHandlerBase createHighlightUsagesHandler(@NotNull Editor editor,
                                                                           @NotNull PsiFile psiFile,
                                                                           @NotNull PsiElement target) {

    if (target.getLanguage() != PuppetLanguage.INSTANCE || DumbService.isDumb(psiFile.getProject())) {
      return null;
    }

    PsiReference reference = TargetElementUtil.findReference(editor);

    if (reference instanceof PuppetPolyVariantCachingReferenceBase) {
      return new PuppetReferenceHighlightUsagesHandler(editor, psiFile, (PuppetPolyVariantCachingReferenceBase)reference);
    }

    PsiElement namedElement =
      TargetElementUtil.getInstance().getNamedElement(target, editor.getCaretModel().getOffset() - target.getTextRange().getStartOffset());

    if (namedElement != null) {
      return new PuppetTargetHighlightUsagesHandler(editor, psiFile, namedElement);
    }

    return null;
  }
}
