package com.intellij.lang.puppet.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetCompositePsiElement;
import com.intellij.lang.puppet.psi.resolve.PuppetResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anna Bulenkova
 */
public class PuppetCompositePsiElementBase extends ASTWrapperPsiElement implements PuppetCompositePsiElement {

  public PuppetCompositePsiElementBase(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public final PsiReference @NotNull [] getReferences() {
    return getReferencesWithCache();
  }

  @Override
  public PsiReference getReference() {
    PsiReference[] refs = getReferences();
    return refs.length > 0 ? refs[0] : null;
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState resolveState,
                                     @Nullable PsiElement lastChildElement,
                                     @NotNull PsiElement originElement) {
    return PuppetResolveUtil.processChildren(this, processor, resolveState, lastChildElement, originElement);
  }
}
