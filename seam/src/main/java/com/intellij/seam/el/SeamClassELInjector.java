package com.intellij.seam.el;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.javaee.el.impl.ELLanguage;
import com.intellij.javaee.el.providers.ELContextProvider;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.seam.facet.SeamFacet;
import com.intellij.seam.utils.SeamCommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class SeamClassELInjector implements MultiHostInjector {

  @Override
  public void getLanguagesToInject(@NotNull final MultiHostRegistrar registrar, @NotNull final PsiElement host) {
    if (!host.textContains('#')) return;
    final PsiElement originalElement = CompletionUtil.getOriginalElement(host);
    PsiClass psiClass = PsiTreeUtil.getParentOfType(originalElement, PsiClass.class);

    if (psiClass != null) {
      final Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
      if (module != null && SeamFacet.getInstance(module) != null) {
        if (SeamCommonUtils.isSeamClass(psiClass) || SeamCommonUtils.isAbstractSeamComponent(psiClass)) {
          for (TextRange textRange : SeamELInjectorUtil.getELTextRanges(host)) {
            registrar.startInjecting(ELLanguage.INSTANCE).addPlace(null, null, (PsiLanguageInjectionHost)host, textRange).doneInjecting();

            host.putUserData(ELContextProvider.ourContextProviderKey, new SeamELContextProvider(host));
          }
        }
      }
    }
  }

  @Override
  @NotNull
  public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Collections.singletonList(PsiLiteral.class);
  }
}
