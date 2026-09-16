package com.intellij.bigdatatools.zeppelin.editor

import com.intellij.bigdatatools.notebooks.core.api.editor.NotebookEditor
import com.intellij.bigdatatools.zeppelin.controllers.editor.ExecutionProgressController
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.ZeppelinClearAllOutputAction
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.ZeppelinExportToHtmlAction
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.ZeppelinRunActionGroup
import com.intellij.bigdatatools.zeppelin.editor.actions.toolbar.ZeppelinStopAllAction
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.jetbrains.bigdatatools.common.ui.CustomComponentActionImpl

class ZeppelinEditor(sourceEditor: TextEditor, editorName: String) : NotebookEditor(sourceEditor, editorName) {
  override val note: ZeppelinNotebook = super.note as ZeppelinNotebook

  val executionProgressController = ExecutionProgressController(this)

  init {
    Disposer.register(this, executionProgressController)
    toolbarActionGroup.addAll(getDefaultActions())

    if (FileDocumentManager.getInstance().getDocument(file.originFile)?.isWritable == false)
      editor.document.setReadOnly(true)

    editor.putUserData(ZEPPELIN_EDITOR, this)
    (sourceEditor.editor as? EditorEx)?.contextMenuGroupId = "ZeppelinEditorPopupMenu"
  }

  override fun getDefaultActions(): List<AnAction> {

    val minorActions = DefaultActionGroup(ZeppelinExportToHtmlAction(this),
                                          ActionManager.getInstance().getAction("BigDataTools.Zeppelin.Toggle.Presentation"),
                                          ActionManager.getInstance().getAction("BigDataTools.Zeppelin.StateViewer.Open")).apply {
      isPopup = true
      templatePresentation.icon = AllIcons.Actions.More
      templatePresentation.text = ZepMessagesBundle.message("action.more")
    }

    return listOfNotNull(
      ZeppelinRunActionGroup(this),
      ZeppelinStopAllAction(this),
      ZeppelinClearAllOutputAction(this),
      Separator.create(),
      CustomComponentActionImpl(executionProgressController.getComponent()),
      ActionManager.getInstance().getAction("GotoParagraph"),
      minorActions
    )
  }

  override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? {
    return getSourceEditor().backgroundHighlighter
  }

  companion object {
    val ZEPPELIN_EDITOR = Key<ZeppelinEditor>("ZEPPELIN_EDITOR")
  }
}