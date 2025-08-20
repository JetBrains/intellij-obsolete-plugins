package com.intellij.play.references;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class PlayRenderViewsPsiReferenceProvider extends PsiReferenceProvider {

  private final PlayControllerActionPsiReferenceProvider myActionProvider = new PlayControllerActionPsiReferenceProvider();
  private final PlayPathViewsPsiReferenceProvider myViewsTemplatesProvider = new PlayPathViewsPsiReferenceProvider();

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    final String text = ElementManipulators.getValueText(element);
    if (StringUtil.isEmptyOrSpaces(text)) return PsiReference.EMPTY_ARRAY;

    return text.startsWith("@")
           ? myActionProvider.getReferencesByElement(element, context)
           : myViewsTemplatesProvider.getReferencesByElement(element, context);
  }
}
