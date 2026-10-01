package com.intellij.dbt.codeInsight

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.impl.source.resolve.ResolveCache


abstract class DbtModelReferenceBase<T : PsiElement>(element: T, textRange: TextRange?) : PsiReferenceBase<T>(element, textRange, true) {
  override fun resolve(): PsiElement? {
    return ResolveCache
      .getInstance(element.getProject())
      .resolveWithCaching(this, MyResolver.INSTANCE, false, false)
  }

  abstract fun resolveInner(): PsiElement?

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    val that = (other as? DbtModelReferenceBase<*>) ?: return false
    return myElement == that.myElement
  }

  override fun hashCode(): Int {
    return myElement.hashCode()
  }

  private class MyResolver : ResolveCache.Resolver {
    override fun resolve(ref: PsiReference, incompleteCode: Boolean): PsiElement? {
      return (ref as DbtModelReferenceBase<*>).resolveInner()
    }

    companion object {
      val INSTANCE = MyResolver()
    }
  }
}

