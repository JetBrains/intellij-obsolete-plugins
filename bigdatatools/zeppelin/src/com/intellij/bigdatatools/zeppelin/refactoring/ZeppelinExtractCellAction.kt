package com.intellij.bigdatatools.zeppelin.refactoring

import com.intellij.bigdatatools.notebooks.core.impl.editor.actions.NotebookEditorActionService
import com.intellij.bigdatatools.zeppelin.editor.NoteActionsIds
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.ZeppelinEditorDumbAwareAction
import com.intellij.bigdatatools.zeppelin.integration.ZeppelinAutoImportUtil
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.database.dialects.base.endOffset
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

internal class ZeppelinExtractCellAction(editor: ZeppelinEditor?) :
  ZeppelinEditorDumbAwareAction(editor,
                                NoteActionsIds.EXTRACT_SCALA,
                                ZepMessagesBundle.message(
                                  "action.extract.single.cell.text"),
                                ZepMessagesBundle.message(
                                  "action.extract.single.cell.description"),
                                RfsIcons.createArrowInscribed(
                                  AllIcons.Actions.ListFiles)) {

  constructor() : this(null)

  override fun actionPerformed(e: AnActionEvent) {
    MyExtractCellJobHandler().invokeFromContext(e)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true
    val data = NotebookEditorActionService.getNotebookExecutionData(e)
    e.presentation.isEnabled = data != null && ZeppelinExtractRefactoringUtil.SUPPORTED_CODES.contains(data.cell.interpreterCode)
  }

  private class MyExtractCellJobHandler : ZeppelinExtractForActionHandler() {

    override fun getTitle() = ZepMessagesBundle.message("action.extract.single.cell.text")

    override fun extractRefactoringElements(file: ScalaFile, editor: Editor): List<PsiElement> {
      val cell = ZeppelinAutoImportUtil.findCell(file as PsiFile, editor.caretModel.offset) ?: return emptyList()
      val start = findActualOffset(cell, file)?.let { ZeppelinExtractRefactoringUtil.getTopLevelElement(it) } ?: return emptyList()
      val end = file.findElementAt(cell.endOffset - 1)?.let { ZeppelinExtractRefactoringUtil.getTopLevelElement(it) }

      return enumerateElements(file, start, end)
    }
  }
}