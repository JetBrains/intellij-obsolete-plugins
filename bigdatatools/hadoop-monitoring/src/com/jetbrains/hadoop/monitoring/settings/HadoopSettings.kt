package com.jetbrains.hadoop.monitoring.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.bigdatatools.common.connection.updater.IntervalUpdateSettings
import com.jetbrains.bigdatatools.common.settings.ColumnVisibilitySettings
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.NodeState
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.YarnApplicationState

/**
 * @author: vitaly.khudobakhshov
 */
@Suppress("MemberVisibilityCanBePrivate")
@State(name = "HadoopSettings", storages = [Storage("hadoop.xml")])
class HadoopSettings : PersistentStateComponent<HadoopSettings>, IntervalUpdateSettings {

  companion object {
    fun getInstance(): HadoopSettings = service()
  }

  override var selectedConnectionId: String? = null
  var applicationColumns = mutableListOf("id", "user", "name", "applicationType", "queue", "state", "finalStatus", "progress",
                                         "trackingUrl", "startedTime", "elapsedTime")
  var applicationDetailsColumns = AppInfo.renderableColumns.map { it.name }.toMutableList()
  var nodesColumns = mutableListOf("rack", "state", "id", "nodeHostName", "nodeHTTPAddress", "lastHealthUpdate", "numContainers",
                                   "usedMemoryMB", "availMemoryMB", "usedVirtualCores", "availableVirtualCores", "version")
  var nodeLabelColumns = mutableListOf("labelName", "labelType", "numActiveNodeMangers", "totalResource")
  var appAttemptColumns = mutableListOf("id", "startTime", "finishedTime", "containerId")
  var containerInfoColumns = mutableListOf("containerId", "nodeId", "containerExitStatus", "logUrl")

  val applicationColumnSettings = ColumnVisibilitySettings(applicationColumns)
  val applicationDetailsColumnSettings = ColumnVisibilitySettings(applicationDetailsColumns)
  val nodeColumnSettings = ColumnVisibilitySettings(nodesColumns)
  val nodeLabelColumnSettings = ColumnVisibilitySettings(nodeLabelColumns)
  val appAttemptColumnSettings = ColumnVisibilitySettings(appAttemptColumns)
  val containerInfoColumnSettings = ColumnVisibilitySettings(containerInfoColumns)

  override var dataUpdateIntervalMillis = 30000

  override var configs: MutableMap<String, HadoopConfig> = mutableMapOf()

  var nodeStates: MutableSet<NodeState> = mutableSetOf<NodeState>().apply { addAll(NodeState.entries.toTypedArray()) }
  var applicationStates: MutableSet<YarnApplicationState> = mutableSetOf<YarnApplicationState>().apply {
    addAll(YarnApplicationState.entries.toTypedArray())
  }

  override fun getState(): HadoopSettings {
    return this
  }

  override fun loadState(state: HadoopSettings) {
    XmlSerializerUtil.copyBean(state, this)

    applicationColumnSettings.visibleColumns = applicationColumns
    applicationDetailsColumnSettings.visibleColumns = applicationDetailsColumns
    nodeColumnSettings.visibleColumns = nodesColumns
    nodeLabelColumnSettings.visibleColumns = nodeLabelColumns
    appAttemptColumnSettings.visibleColumns = appAttemptColumns
    containerInfoColumnSettings.visibleColumns = containerInfoColumns
  }

  fun getOrCreateHadoopConfig(connectionId: String): HadoopConfig {
    var config = configs[connectionId]
    if (config == null) {
      config = HadoopConfig()
      configs[connectionId] = config
    }
    return config
  }

  fun getHadoopConfigOrDefault(connectionId: String): HadoopConfig {
    return configs[connectionId] ?: HadoopConfig()
  }

  fun setSelectedPage(connectionId: String, pageName: String) {
    getOrCreateHadoopConfig(connectionId).selectedPage = pageName
  }

  fun getApplicationDetailsProportion(connectionId: String) = configs[connectionId]?.applicationDetailsProportion ?: 0.5f
  fun setApplicationDetailsProportion(connectionId: String, proportion: Float) {
    getOrCreateHadoopConfig(connectionId).applicationDetailsProportion = proportion
  }

  fun getDetailsAttemptsProportion(connectionId: String) = configs[connectionId]?.detailsAttemptsProportion ?: 0.3f
  fun setDetailsAttemptsProportion(connectionId: String, proportion: Float) {
    getOrCreateHadoopConfig(connectionId).detailsAttemptsProportion = proportion
  }

  fun getExpandedTools(connectionId: String, toolCategory: ToolCategory) = configs[connectionId]?.expandedTools?.contains(toolCategory)
                                                                           ?: false

  fun setExpandedTools(connectionId: String, toolCategory: ToolCategory, expanded: Boolean) {
    if (expanded) {
      getOrCreateHadoopConfig(connectionId).expandedTools.add(toolCategory)
    }
    else {
      getOrCreateHadoopConfig(connectionId).expandedTools.remove(toolCategory)
    }
  }

  fun getScrollLogToBottom(connectionId: String) = configs[connectionId]?.scrollLogToBottom ?: false
  fun setScrollLogToBottom(connectionId: String, value: Boolean) {
    getOrCreateHadoopConfig(connectionId).scrollLogToBottom = value
  }

  fun getLogToContentProportion(connectionId: String) = configs[connectionId]?.logToContentProportion ?: 0.4f
  fun setLogToContentProportion(connectionId: String, proportion: Float) {
    getOrCreateHadoopConfig(connectionId).logToContentProportion = proportion
  }

  fun getStandaloneApplicationProportion(connectionId: String) = configs[connectionId]?.standaloneApplicationProportion ?: 0.25f
  fun setStandaloneApplicationProportion(connectionId: String, proportion: Float) {
    getOrCreateHadoopConfig(connectionId).standaloneApplicationProportion = proportion
  }

  fun getStandaloneDiagnosticsProportion(connectionId: String) = configs[connectionId]?.standaloneDiagnosticsProportion ?: 0.25f
  fun setStandaloneDiagnosticsProportion(connectionId: String, proportion: Float) {
    getOrCreateHadoopConfig(connectionId).standaloneDiagnosticsProportion = proportion
  }

  fun getStandaloneAttemptsProportion(connectionId: String) = configs[connectionId]?.standaloneAttemptsProportion ?: 0.5f
  fun setStandaloneAttemptsProportion(connectionId: String, proportion: Float) {
    getOrCreateHadoopConfig(connectionId).standaloneAttemptsProportion = proportion
  }

  fun isStandaloneDiagnosticsShown(connectionId: String) = configs[connectionId]?.standaloneShowDiagnostics ?: true
  fun setStandaloneDiagnosticsShown(connectionId: String, value: Boolean) {
    getOrCreateHadoopConfig(connectionId).standaloneShowDiagnostics = value
  }

  fun isStandaloneAttemptsShown(connectionId: String) = configs[connectionId]?.standaloneShowAttempts ?: true
  fun setStandaloneAttemptsShown(connectionId: String, value: Boolean) {
    getOrCreateHadoopConfig(connectionId).standaloneShowAttempts = value
  }

  fun isStandaloneContainersShown(connectionId: String) = configs[connectionId]?.standaloneShowContainers ?: true
  fun setStandaloneContainersShown(connectionId: String, value: Boolean) {
    getOrCreateHadoopConfig(connectionId).standaloneShowContainers = value
  }

  fun isAppDetailsShown(connectionId: String) = configs[connectionId]?.appShowDetails ?: true
  fun setAppDetailsShown(connectionId: String, value: Boolean) {
    getOrCreateHadoopConfig(connectionId).appShowDetails = value
  }

  fun isAppAttemptsShown(connectionId: String) = configs[connectionId]?.appShowAttempts ?: true
  fun setAppAttemptsShown(connectionId: String, value: Boolean) {
    getOrCreateHadoopConfig(connectionId).appShowAttempts = value
  }

  fun getScrollDiagnosticsToBottom(connectionId: String) = configs[connectionId]?.scrollDiagnosticsToBottom ?: false
  fun setScrollDiagnosticsToBottom(connectionId: String, value: Boolean) {
    getOrCreateHadoopConfig(connectionId).scrollDiagnosticsToBottom = value
  }
}