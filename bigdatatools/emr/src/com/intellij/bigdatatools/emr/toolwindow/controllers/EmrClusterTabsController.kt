package com.intellij.bigdatatools.emr.toolwindow.controllers

import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TabbedDetailsMonitoringController

class EmrClusterTabsController(dataManager: EmrDataManager, project: Project) : TabbedDetailsMonitoringController<String>(project) {
  override val tabsControllers: List<Pair<String, DetailsMonitoringController<String>>> = listOf(
    EmrMessagesBundle.message("tab.name.info") to EmrClusterInfoController(project, dataManager),
    EmrMessagesBundle.message("tab.name.steps") to EmrClusterStepsController(project, dataManager),
    EmrMessagesBundle.message("tab.name.instances") to EmrClusterInstancesController(project, dataManager),
    EmrMessagesBundle.message("tab.name.applications") to EmrClusterApplicationsController(project, dataManager)
  )

  init {
    init()
  }
}