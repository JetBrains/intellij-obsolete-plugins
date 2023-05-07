package com.intellij.play.usages;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.play.language.psi.PlayPsiFile;
import com.intellij.play.utils.PlayPathUtils;
import com.intellij.play.utils.PlayUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

public class PlayFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
  @Override
  public boolean canFindUsages(@NotNull PsiElement element) {
    return PlayUtils.hasSecondaryElements(element);
  }

  @Override
  public FindUsagesHandler createFindUsagesHandler(@NotNull final PsiElement psiElement, boolean forHighlightUsages) {
    return new FindUsagesHandler(psiElement) {

      @Override
      public PsiElement @NotNull [] getSecondaryElements() {
        if (psiElement instanceof PsiClass) {
          final PsiDirectory directory = PlayPathUtils.getCorrespondingDirectory((PsiClass)psiElement);
          if (directory != null) {
            return new PsiElement[]{directory};
          }
        } else if (psiElement instanceof PsiMethod) {
          final PsiFile view = PlayPathUtils.getCorrespondingView((PsiMethod)psiElement);
          if (view != null) {
            return new PsiElement[]{view};
          }
        }  else if (psiElement instanceof PlayPsiFile) {
          return PlayPathUtils.getCorrespondingControllerMethods((PsiFile)psiElement);
        }

        return super.getSecondaryElements();
      }
    };
  }
}
