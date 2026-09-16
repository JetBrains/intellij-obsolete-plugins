package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.database.sparksql

import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookPsiFile
import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookTemplateDataElementType
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.InterpreterSupportEx
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.ZeppelinTemplateDataElementType
import com.intellij.formatting.Block
import com.intellij.formatting.FormattingMode
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.sql.dialects.base.SqlSyntaxHighlighterFactory
import com.intellij.sql.dialects.spark.SparkDialect
import com.intellij.sql.formatter.model.SqlFormattingModelMaker

class ZeppelinSqlInterpreterSupport : InterpreterSupportEx() {
  override val id: String
    get() = SparkSqlElementTypes.id

  override val language: Language
    get() = SparkDialect.INSTANCE

  override val template: NotebookTemplateDataElementType
    get() = ZeppelinSqlTemplate

  override fun syntaxHighlighter(project: Project?,
                                 virtualFile: VirtualFile?): SyntaxHighlighter =
    SqlSyntaxHighlighterFactory.getSyntaxHighlighter(language, project, virtualFile)

  override fun getFormattingBlocks(notebookPsiFile: NotebookPsiFile, settings: CodeStyleSettings, mode: FormattingMode): List<Block> {
    val constructor = object : BlockConstructor {
      val sqlFile = notebookPsiFile.viewProvider.getPsi(SparkDialect.INSTANCE)!!
      val text = sqlFile.text
      val formattingModel = SqlFormattingModelMaker(text, sqlFile, settings, mode, sqlFile.textRange)

      override fun createBlock(psiElement: PsiElement): Block = formattingModel.makeModel(psiElement.node).subBlocks.first()
    }

    return getFixedLangBlocks(notebookPsiFile, constructor)
  }
}

private object ZeppelinSqlTemplate : ZeppelinTemplateDataElementType(
  "ZEPPELIN_SQL_TEMPLATE",
  SparkSqlElementTypes.source
) {
  override fun getTemplateFileLanguage(viewProvider: TemplateLanguageFileViewProvider?): Language = SparkDialect.INSTANCE
}