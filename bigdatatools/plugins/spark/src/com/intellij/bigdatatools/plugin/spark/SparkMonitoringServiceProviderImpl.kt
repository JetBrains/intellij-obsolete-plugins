package com.intellij.bigdatatools.plugin.spark

import com.intellij.bigdatatools.plugin.spark.services.SparkJobServiceViewContributor
import com.intellij.execution.filters.Filter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.putUserData
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.integration.MonitoringOpenOptions
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.jetbrains.bigdatatools.common.rfs.driver.ActivitySource
import com.jetbrains.bigdatatools.common.rfs.driver.ConnectedConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.settings.ConnectionSettings
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.spark.monitoring.rfs.driver.SparkMonitoringDriver
import com.jetbrains.spark.monitoring.settings.SparkConnectionData
import com.jetbrains.spark.monitoring.settings.SparkConnectionGroup
import com.jetbrains.spark.monitoring.statistics.SparkMonitoringUsagesCollector
import com.jetbrains.spark.monitoring.statistics.ToolbarActionType
import com.jetbrains.spark.monitoring.ui.pages.SparkApplicationConsoleController
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import com.jetbrains.spark.submit.run.ssh.SparkConsoleView
import com.jetbrains.spark.submit.util.SparkAppIdUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException

internal class SparkMonitoringServiceProviderImpl : MonitoringServiceProvider {
  override fun isSupport(groupId: String): Boolean = groupId == BdtConnectionType.SPARK_MONITORING.id

  override fun typeOfMonitoring() = "Spark"

  override fun startApp(project: Project, connData: ConnectionData, name: String, processHandler: ProcessHandler, focusOnApp: Boolean) {
    val driver = (DriverManager.getDriverById(project, connData.innerId) as? SparkMonitoringDriver) ?: return

    val notSyncAppManager = driver.dataManager.notSyncAppManager


    val console = SparkConsoleView(project)
    console.putUserData(SparkApplicationConsoleController.PROCESS_KEY, processHandler)

    console.attachToProcess(processHandler)

    val app = notSyncAppManager.addStartedApp(name, console)

    console.addMessageFilter(object : Filter {
      private var isInited = false
      override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        if (isInited)
          return null

        if (!SparkAppIdUtils.lineHasAppId(line))
          return null


        val appId = SparkAppIdUtils.findByRegex(line)?.value ?: return null
        isInited = true
        notSyncAppManager.updateStartedAppId(app, appId)

        return null
      }
    })

    processHandler.addProcessListener(object : ProcessListener {
      override fun processTerminated(event: ProcessEvent) {
        notSyncAppManager.updateAppIsFinished(app, event.exitCode)
      }
    })

    if (focusOnApp) {
      SparkJobServiceViewContributor.Utils.getInstance()?.focusOnSparkApp(
        project,
        connectionData = connData,
        info = app,
        ignoreIfError = true,
      )
    }
  }

  override fun collectStatistics(project: Project) {
    SparkMonitoringUsagesCollector.toolbarActionInvokedEvent.log(project, ToolbarActionType.OpenUrl)
  }

  override fun createConnection(project: Project, trackingUrl: String, selectOption: MonitoringOpenOptions): ConnectionData? {
    val connectionGroup = SparkConnectionGroup()
    val connectionData = connectionGroup.createBlankData(perProject = selectOption.isPerProject).apply {
      val jobUrl = URL(trackingUrl)
      uri = jobUrl.host + (if (jobUrl.port != -1) ":" + jobUrl.port else "")
      selectOption.tunnelData?.let { setTunnelData(it) }
      name = SMMessagesBundle.message("connection.defaultName", jobUrl.host)
    }
    return ConnectionSettings.create(project, connectionGroup, connectionData, applyIfOk = true)
  }

  override suspend fun waitApplicationAppears(project: Project, trackingApplicationUrl: String, connection: ConnectionData): Boolean {
    val applicationId = extractApplicationId(trackingApplicationUrl)
    try {
      require(connection is SparkConnectionData)
      val existingDriver = DriverManager.getDriverById(project, connection.innerId) as? SparkMonitoringDriver
      val sparkMonitoringDriver = existingDriver ?: connection.createDriver(project, true) as SparkMonitoringDriver

      val dataManager = sparkMonitoringDriver.dataManager
      val client = dataManager.client

      val maxWaitTime = 30 * 1000
      val start = System.currentTimeMillis()

      if (!client.isInited()) {
        val refreshConnection = sparkMonitoringDriver.refreshConnection(ActivitySource.AWAIT_MONITORING)
        refreshConnection.getException()?.let { throwable -> throw throwable }
      }


      while (System.currentTimeMillis() - start < maxWaitTime) {
        withContext(Dispatchers.IO) {
          dataManager.updater.syncRefreshModels(listOf(dataManager.applications))
        }

        if (dataManager.applications.data?.any { it.appId == applicationId } == true) {
          return true
        }
        delay(100)
      }
      return false
    }
    catch (ce: CancellationException) {
      throw ce
    }
    catch (_: Throwable) {
      return false
    }
  }

  override fun openToolWindowController(project: Project, trackingUrl: String, connection: ConnectionData) {
    val applicationId = extractApplicationId(trackingUrl)
    focusOn(project, connection, applicationId, true)
  }

  override fun getConnectionById(project: Project, monitoringDriverId: String): ConnectionData? {
    return RfsConnectionDataManager.instance?.getTyped<SparkConnectionData>(monitoringDriverId, project)
  }

  override fun getConnectionsByProject(project: Project): List<ConnectionData>? {
    return RfsConnectionDataManager.instance?.getTyped<SparkConnectionData>(project)
  }

  override fun openNewConnectionSettings(groupId: String, project: Project, perProject: Boolean): ConnectionData? {
    if (!isSupport(groupId))
      return null

    val connectionGroup = SparkConnectionGroup()
    return ConnectionSettings.create(project, connectionGroup, connectionGroup.createBlankData(perProject = perProject),
                                     applyIfOk = true)
  }

  override fun focusOn(project: Project, connectionData: ConnectionData, appId: String?, ignoreIfError: Boolean) {
    val driver = (DriverManager.getDriverById(project, connectionData.innerId) as? SparkMonitoringDriver) ?: return
    if (ignoreIfError && driver.dataManager.getConnectionStatus() !is ConnectedConnectionStatus)
      return
    driver.coroutineScope.launch {
      withBackgroundProgress(project, SMMessagesBundle.message("open.spark.app.progress.title"), true) {
        val dataManager = driver.dataManager
        val appInfo = if (appId != null)
          dataManager.applications.data?.firstOrNull { it.appId == appId } ?: dataManager.notSyncAppManager.addOutOfFilters(
            appId).firstOrNull()
        else
          null
        SparkJobServiceViewContributor.Utils.getInstance()?.focusOnSparkAppSuspend(project,
                                                                                   connectionData = connectionData,
                                                                                   info = appInfo,
                                                                                   ignoreIfError = ignoreIfError)
      }
    }
  }

  //Sample url: http://<ip_or_name>:<port>/jobs/job/?id=1
  //            http://<ip_or_name>:<port>/proxy/application_1593079285610_0023/
  override fun extractApplicationId(url: String): String? {
    val splitted = url.split("/")
    val filtered = splitted.filter { it.isNotBlank() }
    val last = filtered.lastOrNull() ?: return null
    return if (last.startsWith("app")) last else null
  }
}