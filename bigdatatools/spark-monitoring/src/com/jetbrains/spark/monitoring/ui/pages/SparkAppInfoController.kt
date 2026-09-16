package com.jetbrains.spark.monitoring.ui.pages

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.TabsListener
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.TabbedDetailsMonitoringController
import com.jetbrains.spark.monitoring.data.AppAttemptId
import com.jetbrains.spark.monitoring.models.SparkDataManager
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import java.util.concurrent.atomic.AtomicBoolean

class SparkAppInfoController(project: Project,
                             val dataManager: SparkDataManager,
                             val source: SparkAppControllerSource) : TabbedDetailsMonitoringController<AppAttemptId>(project) {
  val isDisposed = AtomicBoolean(false)
  private val connId = dataManager.connectionData.innerId
  val jobsPageController = JobsPageController(project, connId)

  override val tabsControllers = listOf(
    SMMessagesBundle.message("applications.tab.console") to SparkApplicationConsoleController(project, connId),
    SMMessagesBundle.message("applications.tab.info") to SparkApplicationInfoController(project, connId),
    JOBS_LABEL to jobsPageController,
    SMMessagesBundle.message("applications.tab.stages") to StagesPageController(project, StagesPageController.Type.FULL, connId),
    SMMessagesBundle.message("applications.tab.environment") to EnvironmentPageController(project, connId),
    SMMessagesBundle.message("applications.tab.executors") to SparkExecutorsController(project, connId),
    SMMessagesBundle.message("applications.tab.storage") to StoragePageController(project, connId),
    SMMessagesBundle.message("applications.tab.sql") to SqlPageController(project, connId, this),
  )

  init {
    init()
    tabs.addListener(object : TabsListener {
      override fun selectionChanged(oldSelection: TabInfo?, newSelection: TabInfo?) {
        State.lastSelectedLabel = tabs.selectedInfo?.text
      }
    })
  }

  override fun dispose() {
    isDisposed.set(true)
    super.dispose()
  }

  override fun setDetailsId(id: AppAttemptId) {
    if (isDisposed.get())
      return
    val notSyncAppManager = dataManager.notSyncAppManager
    val consoleView = notSyncAppManager.getConsoleView(notSyncAppManager.getCustomId(id.appId))
    tabs.tabs.first().isHidden = consoleView == null

    selectByLabel(State.lastSelectedLabel)
    super.setDetailsId(id)
  }

  override fun getActions(): List<AnAction> = source.rightToolbarActions

  object State {
    var lastSelectedLabel: String? = null
  }
  companion object {
    val JOBS_LABEL = SMMessagesBundle.message("applications.tab.jobs")
  }
}