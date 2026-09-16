package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.database.hive

import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookTemplateDataElementType
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.InterpreterSupportEx
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.ZeppelinTemplateDataElementType
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.sql.dialects.base.SqlSyntaxHighlighterFactory
import com.intellij.sql.dialects.hive.HiveDialect

internal class ZeppelinHiveInterpreterSupport : InterpreterSupportEx() {
  override val id: String
    get() = HiveElementTypes.id

  override val language: Language
    get() = HiveDialect.INSTANCE

  override val template: NotebookTemplateDataElementType
    get() = ZeppelinHiveTemplate

  override fun syntaxHighlighter(project: Project?,
                                 virtualFile: VirtualFile?): SyntaxHighlighter =
    SqlSyntaxHighlighterFactory.getSyntaxHighlighter(language, project, virtualFile)
}

private object ZeppelinHiveTemplate : ZeppelinTemplateDataElementType(
  "ZEPPELIN_HIVE_TEMPLATE",
  HiveElementTypes.source
) {
  override fun getTemplateFileLanguage(viewProvider: TemplateLanguageFileViewProvider?): Language = HiveDialect.INSTANCE
}