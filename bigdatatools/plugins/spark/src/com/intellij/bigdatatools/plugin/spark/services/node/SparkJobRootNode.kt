package com.intellij.bigdatatools.plugin.spark.services.node

import com.intellij.bigdatatools.emr.settings.EmrConnectionData
import com.intellij.bigdatatools.plugin.spark.BigdatatoolsPluginSparkIcons
import com.intellij.bigdatatools.plugin.spark.arbitrary.ArbitraryClusterConnectionData
import com.intellij.bigdatatools.sparkMonitoring.icons.BigdatatoolsSparkMonitoringIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.services.BdtServiceViewDescriptor
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.bigdatatools.dataproc.settings.DataprocConnectionData
import com.jetbrains.spark.monitoring.settings.SparkConnectionData
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import javax.swing.Icon

class SparkJobRootNode(project: Project) : BdtBaseNode(null, project) {
  override val label: String = SMMessagesBundle.message("services.spark.jobs.title")
  override val icon: Icon = BigdatatoolsSparkMonitoringIcons.Spark

  override fun dispose() {}

  override fun getNodeViewDescriptor(project: Project) = object : BdtServiceViewDescriptor() {
    override fun getContentComponent() = null
    override fun getPresentation() = PresentationData(label, null, icon, null)
    override fun getToolbarActions() = null
    override fun getPopupActions() = null
  }

  override fun getChildren(): List<BdtDriverNode> {
    val connections = RfsConnectionDataManager.instance?.getConnections(project) ?: emptyList()
    val emrConnData = connections.filterIsInstance<EmrConnectionData>()
    val dataprocConnData = connections.filterIsInstance<DataprocConnectionData>()
    val independentSparkConnData = connections.filterIsInstance<SparkConnectionData>()
      .filter { it.sourceConnection == null }

    val arbitraryClusterConnData = connections.filterIsInstance<ArbitraryClusterConnectionData>()
    val arbitrarySparkConnections = arbitraryClusterConnData.map { conn ->
      val sparkConnection = conn.getSparkConnection(project)
      conn to sparkConnection
    }
    val (enabled, disabled) = arbitrarySparkConnections.partition { it.first.isEnabled && it.second != null }

    val bdtDriverNodes =
      emrConnData.map { EmrDriverNode(project, this, it) } +
      dataprocConnData.map { DataprocDriverNode(project, this, it) } +
      independentSparkConnData.map { SparkMonitoringDriverNode(project, this, it) } +
      disabled.map { ArbitraryNotInitedSparkNode(project, this, it.first) } +
      enabled.map {
        SparkMonitoringDriverNode(project, this, it.second!!, label = it.first.name,
                                  icon = BigdatatoolsPluginSparkIcons.ArbitraryCluster)
      }

    val sorted = bdtDriverNodes.sortedWith(compareByDescending<BdtDriverNode> { it.connData.isEnabled }.thenBy { it.label.lowercase() })
    bdtDriverNodes.forEach { Disposer.register(this, it) }

    return sorted
  }
}