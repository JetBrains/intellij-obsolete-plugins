package com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobmanager

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TableWithDetailsMonitoringController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.model.LogFileInfo
import com.jetbrains.bigdatatools.flink.toolwindow.config.FlinkToolWindowSettings
import com.jetbrains.bigdatatools.flink.toolwindow.controllers.ConsoleLogsDetailsMonitoringController

class FlinkJobManagerLogListController(val project: Project,
                                       private val dataManager: FlinkDataManager) : TableWithDetailsMonitoringController<LogFileInfo, String>() {
  override val detailsController = object : ConsoleLogsDetailsMonitoringController(project, dataManager) {
    override val downloadFileName = { selectedId ?: "output.txt" }

    override fun getAdditionalActions() = listOf(
      OpenUrlAction(dataManager) {
        val fileName = selectedId ?: return@OpenUrlAction null
        "/#/job-manager/log/$fileName"
      }
    )

    override fun getDataModel() = selectedId?.let { dataManager.getJobManagerLogFileModel(it) }
  }

  init {
    init()

    Disposer.register(this, detailsController)
  }

  override fun getColumnSettings() = FlinkToolWindowSettings.getInstance().logFileInfoSettings

  override fun getRenderableColumns() = LogFileInfo.renderableColumns

  override fun getDataModel() = dataManager.jobManagerLogsListModel

  override fun showColumnFilter(): Boolean = false

  override fun getAdditionalActions(): List<AnAction> = listOf(OpenUrlAction(dataManager) { "/#/job-manager/log" } )

  override fun indexToDetailId(row: Int) = dataTable.getDataAt(row)?.name ?: ""

  override fun saveSelectedItem() {}
}
