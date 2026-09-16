package com.intellij.bigdatatools.visualization.inlays

import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.util.Key

/**
 * Manages inlays.
 *
 * On project load subscribes
 *    on editor opening/closing.
 *    on adding/removing notebook cells
 *    on any document changes
 *    on folding actions
 *
 * On editor open checks the PSI structure and restores saved inlays.
 *
 * ToDo should be split into InlaysManager with all basics and NotebookInlaysManager with all specific.
 */
class InlaysManagerImpl : InlaysManager, Disposable {

  companion object {
    val EDITOR_INLAYS_MANAGER_KEY = Key.create<EditorInlaysManager>("com.intellij.bigdatatools.visualization.inlays.EditorInlaysManager")
  }

  override fun dispose() {}

  private var initialized = false

  override fun onNotebookOpened(editor: Editor) {

    if (editor.isDisposed) {
      return
    }

    val inlaysManager = EditorInlaysManager.installOn(editor) ?: return
    editor.putUserData(EDITOR_INLAYS_MANAGER_KEY, inlaysManager)

    if (!initialized) {
      EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
        override fun editorReleased(event: EditorFactoryEvent) = onEditorClosed(event.editor)
      }, this)
      initialized = true
    }
  }

  // Message could come multiple time when notebook is closed - this is because we have colored console editor component in inlays.
  private fun onEditorClosed(editor: Editor) {
    val manager = editor.getUserData(EDITOR_INLAYS_MANAGER_KEY)
    manager?.let {
      it.dispose()
      editor.putUserData(EDITOR_INLAYS_MANAGER_KEY, null)
    }
  }

  override fun onProgress(editor: Editor, cell: NotebookCell, percentage: Int) {
    val manager = editor.getUserData(EDITOR_INLAYS_MANAGER_KEY) ?: return
    manager.onProgress(cell, percentage)
  }

  override fun onOutput(editor: Editor, cell: NotebookCell, data: String, update: Boolean, type: CellResultType) {
    val manager = editor.getUserData(EDITOR_INLAYS_MANAGER_KEY) ?: return
    manager.onOutput(cell, data, update, type)
  }

  override fun onParagraphInfo(editor: Editor, cell: NotebookCell, info: Map<String, Any>) {
    //val manager = editor.getUserData(EDITOR_INLAYS_MANAGER_KEY) ?: return
    //manager.onParagraphInfo(cell, info)
  }
}