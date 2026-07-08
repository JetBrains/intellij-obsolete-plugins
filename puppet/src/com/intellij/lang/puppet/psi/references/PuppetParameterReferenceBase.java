package com.intellij.lang.puppet.psi.references;

import com.intellij.lang.puppet.psi.resolve.PuppetNamedPsiElementProcessor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class PuppetParameterReferenceBase extends PuppetPolyVariantCachingReferenceBase<PsiElement> {
  public PuppetParameterReferenceBase(PsiElement psiElement) {
    super(psiElement);
  }

  @Override
  protected final ResolveResult @NotNull [] resolveInner(boolean incompleteCode) {
    String paramName = ElementManipulators.getValueText(myElement);
    if (StringUtil.isEmpty(paramName)) {
      return ResolveResult.EMPTY_ARRAY;
    }

    List<PsiElement> result = new ArrayList<>();

    PuppetNamedPsiElementProcessor processor = (name, element) -> {
      if (StringUtil.equals(name, paramName)) {
        result.add(element);
      }
    };

    processCandidates(paramName, processor);

    return PsiElementResolveResult.createResults(result);
  }

  protected abstract boolean processCandidates(@NotNull String paramName, @NotNull PuppetNamedPsiElementProcessor processor);
}
