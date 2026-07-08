package com.intellij.lang.puppet.psi.references;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.psi.PuppetDataTypesManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;

public class PuppetDataTypeReference extends PuppetPolyVariantCachingReferenceWithFullQualifiedName<PsiElement> {
  public PuppetDataTypeReference(PsiElement psiElement, TextRange range, String fullQualifiedName) {
    super(psiElement, range, fullQualifiedName);
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner(boolean incompleteCode) {

    PsiElement typeElement =
      PuppetDataTypesManager.getDataTypeLightElementByName(myElement.getProject(), StringUtil.toLowerCase(myFullQualifiedName));
    return typeElement == null ? ResolveResult.EMPTY_ARRAY : new ResolveResult[]{new PsiElementResolveResult(typeElement)};
  }

  @Override
  public @NotNull String getPresentableName() {
    return PuppetBundle.message("puppet.type.names.resource_definition");
  }
}
