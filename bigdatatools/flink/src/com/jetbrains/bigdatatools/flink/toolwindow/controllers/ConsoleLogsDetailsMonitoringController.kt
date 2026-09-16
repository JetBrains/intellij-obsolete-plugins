package com.jetbrains.bigdatatools.flink.toolwindow.controllers

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager

abstract class ConsoleLogsDetailsMonitoringController(project: Project,
                                                      dataManager: FlinkDataManager,
                                                      isFormatted: Boolean = false) :
  ConsoleLogsMonitoringController(project, dataManager, isFormatted), DetailsMonitoringController<String> {
  var selectedId: String? = null

  override fun setDetailsId(id: String) {
    selectedId = id

    val model = getDataModel() ?: return
    setupDataModel(model)

    panel.revalidate()
    panel.repaint()
  }
}