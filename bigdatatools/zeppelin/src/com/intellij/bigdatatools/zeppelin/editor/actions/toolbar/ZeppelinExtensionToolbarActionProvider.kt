package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.openapi.extensions.ExtensionPointName

abstract class ZeppelinExtensionToolbarActionProvider {
  abstract fun createActions(editor: ZeppelinEditor): List<ZeppelinEditorDumbAwareAction>

  companion object {
    private const val ID: String = "com.intellij.bigdatatools.zeppelin.extensionToolbarActionProvider"
    private val EP_NAME = ExtensionPointName.create<ZeppelinExtensionToolbarActionProvider>(ID)

    fun createFor(editor: ZeppelinEditor) = EP_NAME.extensionList.flatMap { it.createActions(editor) }
  }
}