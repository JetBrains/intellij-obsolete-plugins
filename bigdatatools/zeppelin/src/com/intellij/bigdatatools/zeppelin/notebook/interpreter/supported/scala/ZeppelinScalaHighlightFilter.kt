package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.scala

import com.intellij.bigdatatools.zeppelin.psi.ZeppelinPsiFile
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiFile

class ZeppelinScalaHighlightFilter : HighlightInfoFilter {
  companion object {
    private const val cannotResolveMessagePrefix = "Cannot resolve symbol res"
  }

  override fun accept(highlightInfo: HighlightInfo, psiFile: PsiFile?): Boolean =
    psiFile !is ZeppelinPsiFile || highlightInfo.severity != HighlightSeverity.ERROR || !isResResolveError(highlightInfo)

  private fun isResResolveError(highlightInfo: HighlightInfo): Boolean {
    if (highlightInfo.description == null) return false

    val description = highlightInfo.description.trim()
    if (!description.startsWith(cannotResolveMessagePrefix)) return false

    return description.substring(cannotResolveMessagePrefix.length).all { it.isDigit() }
  }
}