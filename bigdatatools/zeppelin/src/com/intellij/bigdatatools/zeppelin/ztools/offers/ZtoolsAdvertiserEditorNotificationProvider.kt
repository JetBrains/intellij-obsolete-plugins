// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.ztools.offers

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.file.NotebookFileUtil
import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsPersistentService
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import java.util.function.Function
import javax.swing.JComponent

class ZtoolsAdvertiserEditorNotificationProvider : EditorNotificationProvider, DumbAware {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    return Function { createNotificationPanel(file, it, project) }
  }

  private fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
    if (file.fileType !is ZeppelinFileType) return null
    val editor = fileEditor as? ZeppelinEditor ?: return null
    val configId = NotebookFileUtil.getConfigId(editor.file) ?: return null
    val connData = DriverManager.getDriverById(editor.project, configId)?.connectionData as? ZeppelinConnectionData ?: return null

    val connId = connData.innerId

    if (connId !in ZtoolsOfferService.offerIds)
      return null
    ZtoolsOfferService.offerIds -= connId

    return createPanel(editor, connData)
  }

  private fun createPanel(zeppelinEditor: ZeppelinEditor, connectionData: ZeppelinConnectionData): EditorNotificationPanel {
    val panel = EditorNotificationPanel(EditorNotificationPanel.Status.Info)

    panel.text = ZepMessagesBundle.message("ztools.offer.enable")
    @Suppress("DialogTitleCapitalization")
    panel.createActionLabel(ZepMessagesBundle.message("ztools.offer.enable.new.ztools.action.title")) {
      connectionData.isZtoolsEnabled = true
      NotificationUtils.showInfoMessage(zeppelinEditor.project,
                                           ZepMessagesBundle.message("ztools.offer.enable.new.ztools.success"),
                                           ZepMessagesBundle.message("ztools.offer.enable.new.ztools.success.title"))

    }

    panel.createActionLabel(ZepMessagesBundle.message("plugin.ignore")) {
      ZtoolsPersistentService.getInstance().doNotOfferForConnIds += connectionData.innerId
    }
    return panel
  }
}