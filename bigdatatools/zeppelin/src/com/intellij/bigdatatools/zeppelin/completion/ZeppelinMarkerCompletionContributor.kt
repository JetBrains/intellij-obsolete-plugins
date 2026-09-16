package com.intellij.bigdatatools.zeppelin.completion

import com.intellij.bigdatatools.notebooks.core.api.psi.PsiCellMarker
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.ZeppelinMarkerResolver
import com.intellij.codeInsight.completion.AutoCompletionContext
import com.intellij.codeInsight.completion.AutoCompletionDecision
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext

class ZeppelinMarkerCompletionContributor : CompletionContributor() {
  override fun handleAutoCompletionPossibility(context: AutoCompletionContext): AutoCompletionDecision = autoInsertSingleItem(context)

  init {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement().afterMarker(), MyCompletionProvider)
  }

  private object MyCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
      val file = parameters.originalFile.virtualFile
      val notebookId = NotebookFileUtil.getNotebookId(file)
      val configId = NotebookFileUtil.getConfigId(file)
      val markers = ZeppelinMarkerResolver.getMarkers(configId, notebookId) - ""
      markers.forEach {
        result.addElement(LookupElementBuilder.create(it).withBoldness(true))
      }
    }
  }

  /**
   * Place right after "%" in marker
   */
  private fun PsiElementPattern.Capture<*>.afterMarker(): PsiElementPattern.Capture<out PsiElement> =
    withLanguage(ZeppelinLanguage)
      .and(PlatformPatterns.psiElement().inside(PlatformPatterns.psiElement(PsiCellMarker::class.java)))


  /**
   * Implementation of CompletionContributor#handleAutoCompletionPossibility
   *
   * auto-insert the obvious only case; else show other cases.
   */
  private fun autoInsertSingleItem(context: AutoCompletionContext): AutoCompletionDecision =
    if (context.items.size == 1) {
      AutoCompletionDecision.insertItem(context.items.first())!!
    }
    else {
      AutoCompletionDecision.SHOW_LOOKUP!!
    }
}

