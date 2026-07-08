package com.intellij.lang.puppet.psi.references;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetClassStubsIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;

public class PuppetClassDefinitionReference extends PuppetPolyVariantCachingReferenceWithFullQualifiedName<PsiElement> {
  public PuppetClassDefinitionReference(PsiElement psiElement, TextRange range, String fullQualifiedName) {
    super(psiElement, range, fullQualifiedName);
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner(boolean incompleteCode) {
    if (StringUtil.isEmpty(myFullQualifiedName)) {
      return ResolveResult.EMPTY_ARRAY;
    }

    return PsiElementResolveResult.createResults(PuppetClassStubsIndex.getInstance().find(myFullQualifiedName, getElement()));
  }

  @Override
  public @NotNull String getPresentableName() {
    return PuppetBundle.message("puppet.type.names.class_definition");
  }
}
