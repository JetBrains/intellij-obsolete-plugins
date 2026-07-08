package com.intellij.lang.puppet.ide.usages;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.lang.puppet.psi.PuppetCompositePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetFindUsagesFactory extends FindUsagesHandlerFactory {
  @Override
  public boolean canFindUsages(@NotNull PsiElement element) {
    return element instanceof PsiNamedElement && element instanceof PuppetCompositePsiElement;
  }

  @Override
  public @Nullable FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element, boolean forHighlightUsages) {
    if (!canFindUsages(element)) {
      return null;
    }

    return new PuppetFindUsagesHandler(element);
  }

  private static class PuppetFindUsagesHandler extends FindUsagesHandler {
    PuppetFindUsagesHandler(PsiElement element) {
      super(element);
    }
  }
}
