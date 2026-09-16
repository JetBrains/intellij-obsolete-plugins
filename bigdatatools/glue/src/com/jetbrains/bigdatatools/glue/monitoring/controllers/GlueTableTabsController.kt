package com.jetbrains.bigdatatools.glue.monitoring.controllers

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TabbedDetailsMonitoringController
import com.jetbrains.bigdatatools.glue.client.GlueDataManager
import com.jetbrains.bigdatatools.glue.utils.GlueMessagesBundle

class GlueTableTabsController(project: Project, dataManager: GlueDataManager) : TabbedDetailsMonitoringController<String>(project) {
  override val tabsControllers: List<Pair<String, DetailsMonitoringController<String>>> = listOf(
    GlueMessagesBundle.message("table.tab.info") to GlueTableSummaryController(project, dataManager),
    GlueMessagesBundle.message("table.tab.schema") to GlueTableSchemaController(dataManager),
    GlueMessagesBundle.message("table.tab.partitions") to GlueTablePartitionsController(project, dataManager),
  )

  init {
    init()
  }
}