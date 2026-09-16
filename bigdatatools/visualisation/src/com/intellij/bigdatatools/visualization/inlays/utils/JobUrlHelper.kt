package com.intellij.bigdatatools.visualization.inlays.utils

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.integration.MonitoringOpenOptions
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.jetbrains.bigdatatools.common.notebooks.NotebookFileService
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager

object JobUrlHelper {

  private fun isConnectionPerProject(editor: Editor, project: Project): Boolean {

    //val editorComponent = ComponentUtil.getParentOfType(EditorComponentImpl::class.java as Class<out EditorComponentImpl?>, component)
    //                      ?: return false
    //val editor = editorComponent.editor.virtualFile

    val fileService = NotebookFileService.getAll().firstOrNull() ?: return false
    val file = (editor as EditorImpl).virtualFile ?: return false
    val configId = fileService.getConfigId(file) ?: return false

    val connection = RfsConnectionDataManager.instance?.getConnectionById(project, configId) ?: return false
    return connection.isPerProject
  }

  fun openJobUrl(editor: Editor, project: Project, typeOfJob: String, url: String) {
    val groupId = if (typeOfJob == "spark") BdtConnectionType.SPARK_MONITORING.id else BdtConnectionType.FLINK.id
    MonitoringServiceProvider.openJobUrl(groupId, project, url, null, MonitoringOpenOptions(
      rememberConnection = {},
      isPerProject = isConnectionPerProject(editor, project),
      tunnelData = null
    ))
  }
}