package com.intellij.bigdatatools.zeppelin.formatter

import com.intellij.bigdatatools.notebooks.core.impl.document.NoteDocumentFileUtil
import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookPsiFile
import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage
import com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.raw.RawTextBlock
import com.intellij.formatting.Block
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.psi.formatter.FormattingDocumentModelImpl
import com.intellij.psi.formatter.PsiBasedFormattingModel

class ZeppelinFormattingModelBuilder : FormattingModelBuilder {
  override fun createModel(formattingContext: FormattingContext): FormattingModel {
    val psiElement = formattingContext.psiElement
    val settings = formattingContext.codeStyleSettings
    val mode = formattingContext.formattingMode

    val containingFile = psiElement.containingFile
    val notebookPsiFile = containingFile.viewProvider.getPsi(ZeppelinLanguage) as NotebookPsiFile

    return if (NoteDocumentFileUtil.getIgnoreDocumentChange(containingFile.viewProvider.document!!)) {
      ZeppelinFormattingModel(notebookPsiFile, RawTextBlock(notebookPsiFile.node))
    }
    else
      ZeppelinFormattingModel(notebookPsiFile, RootZeppelinBlock(notebookPsiFile, settings, mode))
  }
}

class ZeppelinFormattingModel(file: NotebookPsiFile, rootBlock: Block) :
  PsiBasedFormattingModel(file, rootBlock, FormattingDocumentModelImpl.createOn(file))