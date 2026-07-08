package com.intellij.lang.puppet.psi.references.providers;

import com.intellij.lang.puppet.psi.PuppetDataTypeParameterInfo;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetVariableReferenceProvider extends PuppetFullQualifiedNamedElementsReferencesProvider {

  @Override
  protected Delegation getLastFqnPartDelegation(@NotNull PsiElement element, String fullQualifiedName) {

    assert element instanceof PuppetVariable;
    return ((PuppetVariable)element).isDeclaration() ? Delegation.NONE : Delegation.VARIABLE;
  }

  @Override
  protected @Nullable PuppetDataTypeParameterInfo getTypeParameterInfo(@NotNull PsiElement element) {
    return null;
  }
}