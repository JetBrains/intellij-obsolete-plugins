package com.intellij.bigdatatools.zeppelin.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.ScopeOptimizer
import com.intellij.psi.search.SearchScope
import org.jetbrains.plugins.scala.ScalaLanguage

class ZeppelinScalaScopeOptimizer : ScopeOptimizer {
  override fun getRestrictedUseScope(element: PsiElement): SearchScope? {
    val psiFile = element.containingFile

    if (psiFile == null || psiFile.viewProvider.allFiles.size < 2 || !psiFile.language.isKindOf(ScalaLanguage.INSTANCE) ||
        psiFile.viewProvider.getPsi(psiFile.viewProvider.baseLanguage) !is ZeppelinPsiFile) return null

    return LocalSearchScope(psiFile)
  }
}