// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.components.containers.editor.connection

import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.file.isNoteFile
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.jetbrains.bigdatatools.common.rfs.driver.ActivitySource
import com.jetbrains.bigdatatools.common.rfs.driver.refreshConnectionLaunch
import java.util.function.Function
import javax.swing.JComponent

class NoConnectionNotificationProvider : EditorNotificationProvider, DumbAware {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!file.isNoteFile) return null
    if (!NotebookFileUtil.isRemote(file)) return null
    if (file.getUserData(ZeppelinNoteConnectionNotificationService.IS_CONNECTED) != false) return null

    val configId = NotebookFileUtil.getConfigId(file) ?: return null
    val driver = ZeppelinDriverManager.getDriver(project, configId) ?: return null
    
    return Function {
      createPanel(driver)
    }
  }
  
  private fun createPanel(driver: ZeppelinDriver): EditorNotificationPanel {
    val panel = EditorNotificationPanel(EditorNotificationPanel.Status.Warning)
    panel.text(ZepMessagesBundle.message("connection.notebook.no.connection"))
    panel.createActionLabel(ZepMessagesBundle.message("connection.notebook.reconnect")) {
      driver.refreshConnectionLaunch(ActivitySource.ACTION)
    }
    return panel
  }
}