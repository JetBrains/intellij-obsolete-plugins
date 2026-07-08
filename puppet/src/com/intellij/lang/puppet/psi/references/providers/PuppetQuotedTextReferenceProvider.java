package com.intellij.lang.puppet.psi.references.providers;

import com.intellij.lang.puppet.psi.PsiPuppetIncludeClassStatement;
import com.intellij.lang.puppet.psi.PuppetElementPatterns;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class PuppetQuotedTextReferenceProvider extends PuppetFullQualifiedNamedElementsReferencesProvider implements PuppetElementPatterns {

  @Override
  protected boolean isFullQualifiedElement(@NotNull PsiElement element) {
    return CLASSNAME_IN_RESOURCE_LIKE_DECLARATION_PATTERN.accepts(element) ||   // class { element:
           element.getParent() instanceof PsiPuppetIncludeClassStatement;       // include statement
  }

  @Override
  protected Delegation getLastFqnPartDelegation(@NotNull PsiElement element, String fullQualifiedName) {
    return Delegation.CLASS;
  }
}
