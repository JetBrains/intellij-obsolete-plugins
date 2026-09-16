// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.components.containers.editor.connection

import com.intellij.bigdatatools.zeppelin.components.containers.controller.ZeppelinNoteController
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.bigdatatools.zeppelin.idea.settings.notebook.bindings.BindingInterpretersView
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import java.util.function.Function
import javax.swing.JComponent

class EmptyBindingsNotificationProvider : EditorNotificationProvider, DumbAware {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (file.fileType != ZeppelinFileType) return null
    val noteId = NotebookFileUtil.getNotebookId(file) ?: return null
    val configId = NotebookFileUtil.getConfigId(file) ?: return null
    val emptyBindingsNotifier = ZeppelinEmptyBindingsController.cachedNotifiers[configId to noteId] ?: return null
    if (emptyBindingsNotifier.hasBindings()) return null

    return Function {
      createPanel(emptyBindingsNotifier.controller)
    }
  }

  private fun createPanel(controller: ZeppelinNoteController): EditorNotificationPanel {
    val panel = EditorNotificationPanel(EditorNotificationPanel.Status.Warning)
    panel.text(ZepMessagesBundle.message("bindings.not.defined"))
    panel.createActionLabel(ZepMessagesBundle.message("bindings.action.setup"), Runnable {
      val isSuccess = BindingInterpretersView.showDialog(controller)
      if (isSuccess) {
        panel.hide()
      }
    })
    return panel
  }
}