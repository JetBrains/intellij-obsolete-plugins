package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.markdown

import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookPsiFile
import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookTemplateDataElementType
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.InterpreterSupportEx
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.ZeppelinTemplateDataElementType
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.templateLanguages.OuterLanguageElement
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import org.intellij.plugins.markdown.highlighting.MarkdownSyntaxHighlighter
import org.intellij.plugins.markdown.lang.MarkdownLanguage

class ZeppelinMarkdownInterpreterSupport : InterpreterSupportEx() {
  override val id: String
    get() = MarkdownElementTypes.id

  override val language: Language
    get() = MarkdownLanguage.INSTANCE

  override val template: NotebookTemplateDataElementType
    get() = ZeppelinMarkdownTemplate

  override fun syntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = MarkdownSyntaxHighlighter()

  override fun getLangPsiElements(notebookPsiFile: NotebookPsiFile): List<PsiElement> {
    val markdownPsiFile = notebookPsiFile.viewProvider.getPsi(language)!!
    return markdownPsiFile.children.filter { it !is OuterLanguageElement && it !is PsiWhiteSpace }
  }
}

private object ZeppelinMarkdownTemplate : ZeppelinTemplateDataElementType(
  "ZEPPELIN_MARKDOWN_TEMPLATE",
  MarkdownElementTypes.source
) {
  override fun getTemplateFileLanguage(
    viewProvider: TemplateLanguageFileViewProvider?): Language = MarkdownLanguage.INSTANCE
}