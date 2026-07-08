package com.intellij.lang.puppet.psi.stubs;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.resolve.PuppetResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PuppetStubBasedPsiElementBase<S extends PuppetStubElement> extends StubBasedPsiElementBase<S>
  implements PuppetStubBasedPsiElement<S> {

  public PuppetStubBasedPsiElementBase(@NotNull S stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetStubBasedPsiElementBase(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public String toString() {
    return getElementType().toString();
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
