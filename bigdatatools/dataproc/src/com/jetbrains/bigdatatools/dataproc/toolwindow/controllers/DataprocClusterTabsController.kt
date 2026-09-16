package com.jetbrains.bigdatatools.dataproc.toolwindow.controllers

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.DetailsMonitoringController
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TabbedDetailsMonitoringController
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle

class DataprocClusterTabsController(dataManager: DataprocDataManager, project: Project) : TabbedDetailsMonitoringController<String>(
  project) {
  override val tabsControllers: List<Pair<String, DetailsMonitoringController<String>>> = listOf(
    DataprocMessagesBundle.message("cluster.tab.info.title") to DataprocClusterInfoController(project, dataManager),
    DataprocMessagesBundle.message("cluster.tab.jobs.title") to DataprocJobsController(project, dataManager),
    DataprocMessagesBundle.message("cluster.tab.vb.instances.title") to DataprocClusterVmInstancesController(project, dataManager),
    DataprocMessagesBundle.message("cluster.tab.applications.title") to DataprocWebInterfacesController(project, dataManager),
  )

  init {
    init()
  }
}