package com.intellij.lang.puppet.ide.refactoring;

import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.lang.puppet.psi.resolve.PuppetResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PuppetRenameProcessor extends RenamePsiElementProcessor {
  @Override
  public boolean canProcessElement(@NotNull PsiElement element) {
    return element instanceof PuppetVariable && ((PuppetVariable)element).isDeclaration() ||
           element.getLanguage() == PuppetLanguage.INSTANCE && element instanceof PsiNamedElement;
  }

  @Override
  public void prepareRenaming(@NotNull PsiElement mainTargetElement, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames, @NotNull SearchScope scope) {
    Set<String> processedNames = new HashSet<>();
    PsiElementProcessor<PsiElement> processor = synonim -> {
      allRenames.putIfAbsent(synonim, newName);
      return true;
    };
    PuppetResolveUtil.processElementSynonims(mainTargetElement, mainTargetElement, processedNames, processor);

    for (PsiReference reference : ReferencesSearch.search(mainTargetElement).asIterable()) {
      if (reference instanceof PsiPolyVariantReference) {
        for (ResolveResult resolveResult : ((PsiPolyVariantReference)reference).multiResolve(false)) {
          PsiElement targetElement = resolveResult.getElement();
          if (targetElement != null && !mainTargetElement.equals(targetElement)) {
            allRenames.put(targetElement, newName);
            PuppetResolveUtil.processElementSynonims(targetElement, mainTargetElement, processedNames, processor);
          }
        }
      }
    }
  }
}
