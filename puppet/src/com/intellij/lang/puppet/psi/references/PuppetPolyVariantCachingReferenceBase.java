package com.intellij.lang.puppet.psi.references;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import org.jetbrains.annotations.NotNull;

public abstract class PuppetPolyVariantCachingReferenceBase<T extends PsiElement> extends PsiPolyVariantReferenceBase<T>
  implements PuppetNamedReference {

  public PuppetPolyVariantCachingReferenceBase(T psiElement) {
    super(psiElement);
  }

  public PuppetPolyVariantCachingReferenceBase(T element, TextRange range) {
    super(element, range);
  }

  @Override
  public final ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    return ResolveCache.getInstance(getElement().getProject()).resolveWithCaching(this, MyResolver.INSTANCE, true, incompleteCode);
  }

  protected abstract ResolveResult @NotNull [] resolveInner(boolean incompleteCode);

  private static class MyResolver implements ResolveCache.PolyVariantResolver<PsiPolyVariantReference> {
    private static final MyResolver INSTANCE = new MyResolver();

    @Override
    public ResolveResult @NotNull [] resolve(@NotNull PsiPolyVariantReference reference, boolean incompleteCode) {
      return ((PuppetPolyVariantCachingReferenceBase<?>)reference).resolveInner(incompleteCode);
    }
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj ||
           this.getClass().equals(obj.getClass()) &&
           getElement() == ((PsiPolyVariantReference)obj).getElement() &&
           getRangeInElement().equals(((PsiPolyVariantReference)obj).getRangeInElement())
      ;
  }

  @Override
  public int hashCode() {
    return getElement().hashCode() * 31 + getRangeInElement().hashCode();
  }

}
