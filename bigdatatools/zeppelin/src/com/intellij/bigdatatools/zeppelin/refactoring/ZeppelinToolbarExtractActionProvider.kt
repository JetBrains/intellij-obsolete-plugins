package com.intellij.bigdatatools.zeppelin.refactoring

import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.ZeppelinEditorDumbAwareAction
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.ZeppelinExtensionToolbarActionProvider

internal class ZeppelinToolbarExtractActionProvider : ZeppelinExtensionToolbarActionProvider() {
  override fun createActions(editor: ZeppelinEditor): List<ZeppelinEditorDumbAwareAction> {
    return listOf(ZeppelinExtractNotebookAction(editor), ZeppelinExtractCellAction(editor))
  }
}