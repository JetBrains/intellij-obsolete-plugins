package com.intellij.lang.puppet.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface PuppetPsiElementWithCachingReferences extends PsiElement {

  /**
   * Shared method to avoid copy-pasting in StubBased, ASTWrapper and Leaf
   *
   */
  default PsiReference[] getReferencesWithCache() {
    return hasReferences() ? CachedValuesManager.getCachedValue(this, () -> {
      List<PsiReference> result = new ArrayList<>();
      computeReferences(result);
      return CachedValueProvider.Result.create(result.toArray(PsiReference.EMPTY_ARRAY), getReferencesCacheDependencies());
    }) : PsiReference.EMPTY_ARRAY;
  }

  /**
   * Returns references dependencies array for CachedValuesManager
   *
   */
  default Object[] getReferencesCacheDependencies() {
    return new Object[]{this};
  }

  /**
   * To avoid excessive poking with references, we will check them only if this flag is true
   *
   */
  default boolean hasReferences() {
    return false;
  }

  /**
   * This calculates references for an element and add them to the result list
   *
   */
  default void computeReferences(List<PsiReference> result) {
    Collections.addAll(result, ReferenceProvidersRegistry.getReferencesFromProviders(this));
  }
}
