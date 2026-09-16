package com.intellij.bigdatatools.notebooks.core.impl.editor.paste

import com.intellij.bigdatatools.notebooks.core.api.NotebookConstants
import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookPsiFile
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class NotebookCopyPasteProcessor : CopyPastePreProcessor {
  override fun preprocessOnCopy(file: PsiFile?, startOffsets: IntArray?, endOffsets: IntArray?, text: String?): String? {
    if (file?.originalFile !is NotebookPsiFile)
      return text

    return text?.replace(NotebookConstants.PARAGRAPH_DELIMITER, "\n")?.replace(NotebookConstants.PARAGRAPH_DELIMITER.removeSuffix("\n"),
                                                                               "\n")
  }

  override fun preprocessOnPaste(project: Project, file: PsiFile, editor: Editor, text: String, rawText: RawText?): String {
    if (file.originalFile !is NotebookPsiFile)
      return text

    return text
      .replace(NotebookConstants.PARAGRAPH_DELIMITER, "\n")
      .replace(NotebookConstants.PARAGRAPH_DELIMITER.removeSuffix("\n"), "\n")
  }
}