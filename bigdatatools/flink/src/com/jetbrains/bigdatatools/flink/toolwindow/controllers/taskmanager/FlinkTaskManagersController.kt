package com.jetbrains.bigdatatools.flink.toolwindow.controllers.taskmanager

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.actions.OpenUrlAction
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TableWithDetailsMonitoringController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.model.TaskManagerInfo
import com.jetbrains.bigdatatools.flink.toolwindow.config.FlinkToolWindowSettings

class FlinkTaskManagersController(project: Project,
                                  val dataManager: FlinkDataManager) : TableWithDetailsMonitoringController<TaskManagerInfo, String>() {
  override val detailsController = FlinkTaskManagerTabbedDetailsController(project, dataManager)

  init {
    init()
  }

  override fun getColumnSettings() = FlinkToolWindowSettings.getInstance().taskManagerSettings

  override fun getRenderableColumns() = TaskManagerInfo.renderableColumns

  override fun getDataModel() = dataManager.taskManagersModel

  override fun showColumnFilter(): Boolean = false

  override fun indexToDetailId(row: Int): String? = dataTable.getDataAt(row)?.id

  override fun saveSelectedItem() {}

  override fun getAdditionalActions(): List<AnAction> = listOf(OpenUrlAction(dataManager) { "/#/task-manager" } )
}