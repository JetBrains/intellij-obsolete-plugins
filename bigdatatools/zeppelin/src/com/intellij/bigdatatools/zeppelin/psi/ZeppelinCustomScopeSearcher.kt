package com.intellij.bigdatatools.zeppelin.psi

import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.plugins.scala.ScalaLanguage

class ZeppelinCustomScopeSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {
  override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
    if (queryParameters.scopeDeterminedByUser is ZeppelinLocalSearchScope) return

    val containingFile = ApplicationManager.getApplication().runReadAction (
      Computable {
        queryParameters.elementToSearch.containingFile
      }
    )

    if (containingFile == null || containingFile.language != ScalaLanguage.INSTANCE || containingFile.viewProvider.getPsi(
        ZeppelinLanguage) == null) return

    val ignoreInjectedPsi = (queryParameters.scopeDeterminedByUser as? LocalSearchScope)?.isIgnoreInjectedPsi ?: false
    ReferencesSearch.searchOptimized(queryParameters.elementToSearch, ZeppelinLocalSearchScope(containingFile, ignoreInjectedPsi), true,
                                     queryParameters.optimizer, consumer)
  }

  class ZeppelinLocalSearchScope(scope: PsiElement, ignoreInjectedPsi: Boolean) :
    LocalSearchScope(arrayOf(scope), null, ignoreInjectedPsi)
}