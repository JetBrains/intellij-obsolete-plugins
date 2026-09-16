package com.intellij.bigdatatools.zeppelin.refactoring

import com.intellij.bigdatatools.notebooks.core.impl.controllers.NoteUsedInterpreterCollector
import com.intellij.bigdatatools.zeppelin.editor.NoteActionsIds
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.ZeppelinEditorDumbAwareAction
import com.intellij.bigdatatools.zeppelin.integration.ZeppelinAutoImportUtil
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile

internal class ZeppelinExtractNotebookAction(editor: ZeppelinEditor?) :
  ZeppelinEditorDumbAwareAction(editor,
                                NoteActionsIds.EXTRACT_SCALA,
                                ZepMessagesBundle.message("action.extract.whole.notebook.text"),
                                ZepMessagesBundle.message("action.extract.whole.notebook.description"),
                                RfsIcons.createArrowInscribed(AllIcons.Actions.Annotate)) {
  constructor() : this(null)

  override fun actionPerformed(e: AnActionEvent) {
    MyExtractNotebookJobHandler().invokeFromContext(e)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true

    val actualEditor = actualContext(e)?.first
    if (actualEditor == null) {
      e.presentation.isEnabled = false
      return
    }

    val usedSubInterpreters = NoteUsedInterpreterCollector.getInstance(actualEditor)?.usedSubInterpreters
    e.presentation.isEnabled = usedSubInterpreters?.any { ZeppelinExtractRefactoringUtil.SUPPORTED_CODES.contains(it) } == true
  }

  private class MyExtractNotebookJobHandler : ZeppelinExtractForActionHandler() {

    override fun getTitle() = ZepMessagesBundle.message("action.extract.whole.notebook.text")

    override fun extractRefactoringElements(file: ScalaFile, editor: Editor): List<PsiElement> {
      val cell = ZeppelinAutoImportUtil.findFirstCell(file as PsiFile) ?: return emptyList()
      val start = findActualOffset(cell, file)?.let { ZeppelinExtractRefactoringUtil.getTopLevelElement(it) } ?: return emptyList()

      return enumerateElements(file, start, null)
    }
  }
}