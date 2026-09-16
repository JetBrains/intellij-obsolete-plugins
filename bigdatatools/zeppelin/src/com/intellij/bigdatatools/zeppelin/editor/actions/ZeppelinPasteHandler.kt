package com.intellij.bigdatatools.zeppelin.editor.actions

import com.intellij.bigdatatools.zeppelin.psi.ZeppelinPsiFile
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.CodeInsightSettings.NO_REFORMAT
import com.intellij.codeInsight.editorActions.PasteHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorTextInsertHandler
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.Producer
import java.awt.datatransfer.Transferable

class ZeppelinPasteHandler(private val originalAction: EditorActionHandler) : PasteHandler(originalAction) {

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }

  override fun execute(editor: Editor, dataContext: DataContext?, producer: Producer<out Transferable>?) {
    val project = editor.project
    if (project == null) {
      return executeOriginal(editor, dataContext, producer)
    }

    val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
    if (file !is ZeppelinPsiFile) {
      return executeOriginal(editor, dataContext, producer)
    }

    val settings = CodeInsightSettings.getInstance()
    val oldReformat = settings.REFORMAT_ON_PASTE
    settings.REFORMAT_ON_PASTE = NO_REFORMAT
    try {
      super.execute(editor, dataContext, producer)
    }
    catch (e: Exception) {
      logger.error(e)
    }
    settings.REFORMAT_ON_PASTE = oldReformat
  }

  internal fun executeOriginal(editor: Editor, dataContext: DataContext?, producer: Producer<out Transferable>?) {
    when (originalAction) {
      is EditorTextInsertHandler -> originalAction.execute(editor, dataContext, producer)
      else -> originalAction.execute(editor, null, dataContext)
    }
  }
}