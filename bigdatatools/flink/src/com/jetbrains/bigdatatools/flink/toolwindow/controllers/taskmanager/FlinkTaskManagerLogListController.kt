package com.jetbrains.bigdatatools.flink.toolwindow.controllers.taskmanager

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsTableMonitoringController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.model.LogFileInfo
import com.jetbrains.bigdatatools.flink.toolwindow.config.FlinkToolWindowSettings
import com.jetbrains.bigdatatools.flink.toolwindow.controllers.ConsoleLogsDetailsMonitoringController
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

class FlinkTaskManagerLogListController(project: Project,
                                        val dataManager: FlinkDataManager) : DetailsTableMonitoringController<LogFileInfo, String>() {
  private val detailsSplitter: OnePixelSplitter = OnePixelSplitter()

  private val getTaskManagerId: () -> String = { selectedId ?: "" }

  private val detailsController = object : ConsoleLogsDetailsMonitoringController(project, dataManager) {
    override val downloadFileName = { selectedId ?: "output.txt" }

    override fun getAdditionalActions() = listOf(
      OpenUrlAction(dataManager) {
        val fileName = selectedId ?: return@OpenUrlAction null
        "/#/task-manager/${getTaskManagerId()}/log-list/$fileName"
      }
    )

    override fun getDataModel() = selectedId?.let { dataManager.getTaskManagersLogFileModel(getTaskManagerId(), it) }
  }

  private val selectionListener = object : ListSelectionListener {
    override fun valueChanged(e: ListSelectionEvent) {
      if (e.valueIsAdjusting)
        return
      showDetails()
    }
  }

  init {
    super.init()

    Disposer.register(this, detailsController)

    dataTable.selectionModel.addListSelectionListener(selectionListener)

    detailsSplitter.firstComponent = super.getComponent()
    detailsSplitter.secondComponent = detailsController.getComponent()

    showDetails()
  }

  override fun getComponent() = detailsSplitter

  private fun showDetails() {
    indexToDetailId(dataTable.selectedRow).let { detailsController.setDetailsId(it) }
  }

  private fun indexToDetailId(row: Int) = dataTable.getDataAt(row)?.name ?: ""

  override fun getColumnSettings() = FlinkToolWindowSettings.getInstance().logFileInfoSettings

  override fun getRenderableColumns() = LogFileInfo.renderableColumns

  override fun getDataModel() = selectedId?.let { dataManager.getTaskManagerLogListModel(it) }

  override fun showColumnFilter(): Boolean = false

  override fun getAdditionalActions(): List<AnAction> = listOf(
    OpenUrlAction(dataManager) {
      val taskManagersId = selectedId ?: return@OpenUrlAction null
      "/#/task-manager/$taskManagersId/log-list"
    }
  )
}