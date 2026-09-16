package com.intellij.bigdatatools.zeppelin.editor.util

import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.file.isNoteFile
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

object ZeppelinEditorUtil {
  fun getPsiOpenedFile(project: Project, configId: String, noteId: String): PsiFile? {
    val virtualFile = getOpenedVirtualFile(project, configId, noteId) ?: return null
    return invokeAndWaitIfNeeded { PsiManager.getInstance(project).findFile(virtualFile) }
  }

  fun goToParagraph(editor: Editor, note: ZeppelinNotebook, paragraphId: String) {
    val cell = note.cells.find { it.id == paragraphId } ?: return
    NotebookEditorUtils.goToCell(editor, cell)
  }

  private fun getOpenedVirtualFile(project: Project, configId: String, noteId: String): VirtualFile? =
    FileEditorManager.getInstance(project).openFiles.firstOrNull {
      if (!it.isNoteFile) return@firstOrNull false
      val fileConfigId = NotebookFileUtil.getConfigId(it) ?: return@firstOrNull false
      val fileNoteId = NotebookFileUtil.getNotebookId(it) ?: return@firstOrNull false
      fileConfigId == configId && noteId == fileNoteId
    }
}