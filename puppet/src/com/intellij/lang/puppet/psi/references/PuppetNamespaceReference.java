package com.intellij.lang.puppet.psi.references;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetClassStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetNamespacesStubsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetTypeStubIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PuppetNamespaceReference extends PuppetPolyVariantCachingReferenceWithFullQualifiedName<PsiElement> {

  public PuppetNamespaceReference(PsiElement psiElement, String fullQualifiedName) {
    super(psiElement, fullQualifiedName);
  }

  public PuppetNamespaceReference(PsiElement psiElement, TextRange range, String fullQualifiedName) {
    super(psiElement, range, fullQualifiedName);
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner(boolean incompleteCode) {
    if (StringUtil.isEmpty(myFullQualifiedName)) {
      return ResolveResult.EMPTY_ARRAY;
    }

    PsiElement element = getElement();

    // fixme here we should check for module and subdir in manifests with such name and bind to it

    List<PsiElement> targets = new ArrayList<>(PuppetClassStubsIndex.getInstance().find(myFullQualifiedName, element));
    targets.addAll(PuppetTypeStubIndex.getInstance().find(myFullQualifiedName, element));

    if (targets.isEmpty()) {
      targets.addAll(PuppetNamespacesStubsIndex.getInstance().find(myFullQualifiedName, element));
    }

    targets.remove(myElement);

    return PsiElementResolveResult.createResults(targets);
  }

  @Override
  public @NotNull String getPresentableName() {
    return PuppetBundle.message("puppet.type.names.namespace_definition");
  }
}
