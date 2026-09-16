@file:Suppress("DEPRECATION")

package com.jetbrains.spark.monitoring.models

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.intellij.bigdatatools.coreUi.util.BdIdeRegistryUtil
import com.intellij.bigdatatools.coreUi.util.executeNotOnEdt
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.observable.util.whenDisposed
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.jetbrains.bigdatatools.common.rfs.util.RfsNotificationUtils
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.spark.monitoring.data.AppAttemptId
import com.jetbrains.spark.monitoring.data.ApplicationStatus
import com.jetbrains.spark.monitoring.data.PresentableApplicationInfo
import com.jetbrains.spark.monitoring.settings.SparkToolwindowSettings
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class SparkNotSyncAppManager(val dataManager: SparkDataManager) : Disposable {
  private val scope: CoroutineScope
    get() = dataManager.driver.safeExecutor.coroutineScope

  val id = AtomicInteger(0)
  private val isDisposed = AtomicBoolean(false)
  var notSyncApps = setOf<PresentableApplicationInfo>()
    private set

  @Suppress("DEPRECATION")
  private val consoleCache: Cache<Int, ConsoleView> = CacheBuilder.newBuilder()
    .maximumSize(100)
    .expireAfterAccess(Duration.ofHours(1))
    .removalListener<Int, ConsoleView> {
      if (it.wasEvicted()) {
        val console = it.value ?: return@removalListener
        Disposer.dispose(console)
      }
    }
    .build()

  private val appIdToCustomIdCache = mutableMapOf<String, Int>()


  override fun dispose() {
    isDisposed.set(true)
  }

  fun addStartedApp(appName: String, console: ConsoleView): PresentableApplicationInfo {
    val customId = id.incrementAndGet()
    val appId = "$INITIALIZING_PREFIX${customId}"
    appIdToCustomIdCache[appId] = customId
    consoleCache.put(customId, console)

    console.whenDisposed {
      consoleCache.invalidate(customId)
      executeNotOnEdt {
        dataManager.updater.invokeRefreshModel(dataManager.applications)
      }
    }

    val info = PresentableApplicationInfo(appId = appId, name = appName, startTime = Date(),
                                          status = ApplicationStatus.STARTING,
                                          customId = customId)
    notSyncApps += info
    dataManager.updater.syncRefreshModels(listOf(dataManager.applications))
    return info
  }

  fun updateAppIsFinished(app: PresentableApplicationInfo, exitCode: Int) {
    val foundApp = notSyncApps.firstOrNull { it.customId == app.customId } ?: return
    notSyncApps -= foundApp
    val endTime = Date()
    notSyncApps += foundApp.copy(status = if (exitCode == 0) ApplicationStatus.COMPLETE else ApplicationStatus.ERROR, endTime = endTime)
    dataManager.updater.invokeRefreshModel(dataManager.applications)
  }

  @RequiresBackgroundThread
  fun addOutOfFilters(appId: String): List<PresentableApplicationInfo> {
    val appInfos = try {
      val application = dataManager.client.getApplication(appId)
      PresentableApplicationInfo.createFor(application, dataManager)
    }
    catch (t: Throwable) {
      return emptyList()
    }
    notSyncApps += appInfos
    dataManager.updater.syncRefreshModels(listOf(dataManager.applications))
    return appInfos
  }

  fun updateStartedAppId(app: PresentableApplicationInfo, appId: String) {
    val customId = app.customId ?: let {
      thisLogger().warn("Custom ID is not found")
      return
    }
    notSyncApps -= app
    notSyncApps += app.copy(appId = appId)
    appIdToCustomIdCache[appId] = customId
    dataManager.updater.invokeRefreshModels(listOf(dataManager.applications, dataManager.applicationInfos[AppAttemptId(appId, null)]))
    waitForInit(appId)
  }

  fun getNotSyncApps(serverApps: List<PresentableApplicationInfo>,
                     states: MutableSet<ApplicationStatus>): List<PresentableApplicationInfo> {
    val serverIds = serverApps.map { it.appId }.toSet()

    val alreadySyncApps = notSyncApps.filter { it.appId in serverIds }
    notSyncApps = notSyncApps - alreadySyncApps.toSet()

    alreadySyncApps.forEach { app ->
      val projects = dataManager.project?.let { listOf(it) } ?: ProjectManager.getInstance().openProjects.toList()
      projects.forEach {
        invokeLater {
          notifyAppearedIfRequired(app.appId, it)
        }
      }
    }

    val modelsForRefresh = alreadySyncApps.flatMap {
      listOf(dataManager.applicationInfos[it.id],
             dataManager.getJobsModel(it.id),
             dataManager.getEnvironmentModel(it.id),
             dataManager.getSqlModel(it.id),
             dataManager.getStoragesModel(it.id),
             dataManager.getStagesModel(StagesDataId(it.id)),
             dataManager.getExecutorsModel(it.id)
      )
    }
    if (modelsForRefresh.isNotEmpty())
      dataManager.updater.invokeRefreshModels(modelsForRefresh)

    val fullStates = states + ApplicationStatus.UNKNOWN + ApplicationStatus.STARTING

    return notSyncApps.filter {
      it.status == ApplicationStatus.UNKNOWN || it.status in fullStates
    }.map {
      it.copy(console = getConsoleView(it.customId))
    }
  }

  private fun waitForInit(appId: String) = scope.launch {
    var refreshRange = 1000L
    val startTime = System.currentTimeMillis()

    while (shouldContinueRefresh(refreshRange, startTime)) {
      val randomTimeout = Random.nextLong(refreshRange)
      delay(randomTimeout)
      refreshRange = 2

      if (!notSyncApps.any { it.appId == appId })
        return@launch

      val apps = try {
        dataManager.client.getApplications(limit = "100")
      }
      catch (t: Throwable) {
        thisLogger().info("Cannot get spark apps", t)
        continue
      }
      val isFound = apps.any { it.id == appId }
      if (isFound) {
        dataManager.updater.invokeRefreshModel(dataManager.applications)
        return@launch
      }
    }
  }

  private fun notifyAppearedIfRequired(appId: String, project: Project) {
    if (ToolWindowManager.getInstance(project).lastActiveToolWindowId == ToolWindowId.SERVICES)
      return

    RfsNotificationUtils.notifyBalloonText(
      project = project,
      message = SMMessagesBundle.message("notify.spark.in.history.message", appId),
      displayGroup = "SparkJobs",
      actions = listOf(object : DumbAwareAction(SMMessagesBundle.message("open.app.in.services")) {
        override fun actionPerformed(e: AnActionEvent) {
          MonitoringServiceProvider.getSparkMonitoringService()?.focusOn(project, dataManager.connectionData, appId, false)
        }

      })
    )
  }


  private fun shouldContinueRefresh(refreshRange: Long, startTime: Long): Boolean {
    val isLessUpdateIntervalFromMonitoringRefreshInterval = SparkToolwindowSettings.getInstance().dataUpdateIntervalMillis > 0 &&
                                                            refreshRange < SparkToolwindowSettings.getInstance().dataUpdateIntervalMillis
    val isLessTimeout = SparkToolwindowSettings.getInstance().dataUpdateIntervalMillis <= 1 &&
                        System.currentTimeMillis() - startTime < BdIdeRegistryUtil.SPARK_HISTORY_APP_WAIT_TIME

    return isLessUpdateIntervalFromMonitoringRefreshInterval || isLessTimeout
  }

  fun getCustomId(id: String) = appIdToCustomIdCache[id]
  fun getConsoleView(id: Int?): ConsoleView? = id?.let { consoleCache.getIfPresent(id) }

  companion object {
    private const val INITIALIZING_PREFIX = "initializing_"
  }
}