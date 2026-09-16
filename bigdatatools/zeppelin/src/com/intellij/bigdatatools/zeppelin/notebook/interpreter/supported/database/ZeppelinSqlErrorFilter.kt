package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.database

import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.bigdatatools.zeppelin.notebook.parser.ZeppelinFileViewProvider
import com.intellij.codeInsight.highlighting.HighlightErrorFilter
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.sql.psi.SqlFile

class ZeppelinSqlErrorFilter : HighlightErrorFilter() {
  override fun shouldHighlightErrorElement(element: PsiErrorElement): Boolean {
    val file = element.containingFile
    return file !is SqlFile || file.viewProvider !is ZeppelinFileViewProvider
  }
}

class ZeppelinSqlInspectionSuppressor : InspectionSuppressor {
  override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean =
    toolId == "SqlResolve" && element.containingFile.fileType is ZeppelinFileType

  override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> = arrayOf()
}