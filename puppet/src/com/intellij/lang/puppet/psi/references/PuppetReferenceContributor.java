package com.intellij.lang.puppet.psi.references;

import com.intellij.lang.puppet.psi.PuppetElementPatterns;
import com.intellij.lang.puppet.psi.PuppetNamespaceDefinition;
import com.intellij.lang.puppet.psi.references.providers.PuppetCapitalizedNameReferenceProvider;
import com.intellij.lang.puppet.psi.references.providers.PuppetQuotedTextReferenceProvider;
import com.intellij.lang.puppet.psi.references.providers.PuppetRegularNameWrapperReferenceProvider;
import com.intellij.lang.puppet.psi.references.providers.PuppetVariableReferenceProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class PuppetReferenceContributor extends PsiReferenceContributor implements PuppetElementPatterns {
  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {

    registrar.registerReferenceProvider(QUOTED_TEXT_PATTERN, new PuppetQuotedTextReferenceProvider());
    registrar.registerReferenceProvider(REGULAR_NAME_WRAPPER_PATTERN, new PuppetRegularNameWrapperReferenceProvider());
    registrar.registerReferenceProvider(CAPITALIZED_NAME_WRAPPER_PATTERN, new PuppetCapitalizedNameReferenceProvider());
    registrar.registerReferenceProvider(VARIABLE_PATTERN, new PuppetVariableReferenceProvider());
    registrar.registerReferenceProvider(NAMESPACE_DEFINITION_PATTERN, new PsiReferenceProvider() {
      @Override
      public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        assert element instanceof PuppetNamespaceDefinition;
        return new PsiReference[]{new PuppetNamespaceReference(element, ((PuppetNamespaceDefinition)element).getFullQualifiedName())};
      }
    });

  }
}
