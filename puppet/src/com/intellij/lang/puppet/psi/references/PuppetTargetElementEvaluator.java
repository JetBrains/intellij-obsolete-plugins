package com.intellij.lang.puppet.psi.references;

import com.intellij.codeInsight.TargetElementEvaluatorEx2;
import com.intellij.lang.puppet.psi.PuppetDelegatingLightNamedElement;
import com.intellij.lang.puppet.psi.PuppetPolyNamedPsiElement;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetTargetElementEvaluator extends TargetElementEvaluatorEx2 {
  @Override
  public boolean includeSelfInGotoImplementation(@NotNull PsiElement element) {
    return false;
  }

  @Override
  public @Nullable PsiElement getElementByReference(@NotNull PsiReference ref, int flags) {
    if (ref instanceof PuppetPolyVariantCachingReferenceBase) {
      ResolveResult[] results = ((PuppetPolyVariantCachingReferenceBase<?>)ref).multiResolve(false);
      if (results.length > 0) {
        return results[0].getElement();
      }
    }
    return null;
  }

  @Override
  public @Nullable PsiElement getNamedElement(@NotNull PsiElement element) {
    PuppetPolyNamedPsiElement polyNamedElement = PsiTreeUtil.getParentOfType(element, PuppetPolyNamedPsiElement.class);
    if (polyNamedElement == null) {
      return null;
    }

    for (PuppetDelegatingLightNamedElement lightNamedElement : polyNamedElement.getLightElementsList()) {
      PsiElement identifier = lightNamedElement.getNameIdentifier();
      if (identifier != null && identifier.getTextRange().contains(element.getTextRange())) {
        return lightNamedElement;
      }
    }
    return null;
  }

  @Override
  public boolean isAcceptableNamedParent(@NotNull PsiElement parent) {
    if (parent instanceof PuppetVariable) {
      return ((PuppetVariable)parent).isDeclaration();
    }
    if (parent instanceof PsiFile) {
      return false;
    }
    return super.isAcceptableNamedParent(parent);
  }
}
