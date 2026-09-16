package com.intellij.bigdatatools.emr.toolwindow.controllers

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.model.EmrClusterAppInfo
import com.intellij.bigdatatools.emr.settings.EmrToolWindowSettings
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsTableMonitoringController
import com.jetbrains.bigdatatools.common.table.renderers.LinkRenderer

class EmrClusterApplicationsController(val project: Project,
                                       private val dataManager: EmrDataManager) : DetailsTableMonitoringController<EmrClusterAppInfo, String>() {
  init {
    init()
  }

  override fun showColumnFilter(): Boolean = false

  override fun getColumnSettings() = EmrToolWindowSettings.getInstance().clustersAppsColumnsSettings

  override fun getRenderableColumns() = EmrClusterAppInfo.renderableColumns

  override fun getDataModel() = selectedId?.let { dataManager.getClusterAppsDataModel(it) }

  override fun getAdditionalActions(): List<AnAction> = listOf()

  override fun customTableInit(table: DataTable<EmrClusterAppInfo>) {
    LinkRenderer.installOnColumn(table, columnModel.getColumn(0)).apply {
      condition = { _, _, row, _ ->
        table.getDataAt(row)?.url?.isNotBlank() == true
      }

      onClick = { row, _ ->
        showConnection(row)
      }
    }
  }

  @Suppress("DuplicatedCode")
  private fun showConnection(row: Int) {
    val appInfo = dataTable.getDataAt(row) ?: return

    val clusterId = selectedId ?: return
    val cluster = dataManager.getClusterDetails(clusterId)

    if (!dataManager.isClusterRun(clusterId)) {
      NotificationUtils.showInfoMessage(project, EmrMessagesBundle.message("error.cluster.must.be.started"),
                                           EmrMessagesBundle.message("error.title"))
      return
    }

    dataManager.dependsManager.browseOrOpenConnection(project, cluster, appInfo)
  }
}