package com.jetbrains.bigdatatools.dataproc.toolwindow.controllers

import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.table.DataTable
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsTableMonitoringController
import com.jetbrains.bigdatatools.common.table.renderers.LinkRenderer
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManagerUtils
import com.jetbrains.bigdatatools.dataproc.model.DataprocWebInterfaceInfo
import com.jetbrains.bigdatatools.dataproc.settings.DataprocToolWindowSettings
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle

class DataprocWebInterfacesController(val project: Project,
                                      private val dataManager: DataprocDataManager) : DetailsTableMonitoringController<DataprocWebInterfaceInfo, String>() {
  init {
    init()
  }

  override fun showColumnFilter(): Boolean = false
  override fun getColumnSettings() = DataprocToolWindowSettings.getInstance().webIntefracesColumnsSettings
  override fun getRenderableColumns() = DataprocWebInterfaceInfo.renderableColumns
  override fun getDataModel() = selectedId?.let { dataManager.geWebInterfacesDataModel(it) }

  override fun customTableInit(table: DataTable<DataprocWebInterfaceInfo>) {
    LinkRenderer.installOnColumn(table, columnModel.getColumn(0)).apply {
      condition = { _, _, row, _ ->
        table.getDataAt(row)?.url?.isNotBlank() == true
      }

      onClick = { row, _ ->
        executeOnPooledThread {
          showConnection(row)
        }
      }
    }
  }

  @Suppress("DuplicatedCode")
  private fun showConnection(row: Int) {
    try {
      val appInfo = dataTable.getDataAt(row) ?: return
      val clusterName = selectedId ?: return

      if (!DataprocDataManagerUtils.checkIsCliOperationsAllowed(project, dataManager.connectionData))
        return

      if (!dataManager.isClusterRun(clusterName)) {
        NotificationUtils.showInfoMessage(project, DataprocMessagesBundle.message("dataproc.error.cluster.must.be.started"),
                                             DataprocMessagesBundle.message("dataproc.error"))
        return
      }
      val cluster = dataManager.getClusterByName(clusterName) ?: return

      dataManager.dependsManager.browseOrOpenConnection(project, cluster, appInfo)
    }
    catch (t: Throwable) {
      NotificationUtils.showExceptionMessage(project, t, title = HdfsMessagesBundle.message("emr.connection.creation"))
    }
  }
}

