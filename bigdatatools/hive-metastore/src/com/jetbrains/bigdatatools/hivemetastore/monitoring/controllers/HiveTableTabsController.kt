package com.jetbrains.bigdatatools.hivemetastore.monitoring.controllers

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TabbedDetailsMonitoringController
import com.jetbrains.bigdatatools.hivemetastore.client.HiveDataManager
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMessagesBundle

class HiveTableTabsController(project: Project, dataManager: HiveDataManager) : TabbedDetailsMonitoringController<String>(project) {
  override val tabsControllers: List<Pair<String, DetailsMonitoringController<String>>> = listOf(
    HiveMessagesBundle.message("table.tab.info") to HiveTableSummaryController(project, dataManager),
    HiveMessagesBundle.message("table.tab.schema") to HiveTableSchemaController(dataManager),
    HiveMessagesBundle.message("table.tab.partitions") to HiveTablePartitionsController(project, dataManager),
  )

  init {
    init()
  }
}