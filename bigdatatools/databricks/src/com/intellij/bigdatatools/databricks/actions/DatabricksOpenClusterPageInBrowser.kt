package com.intellij.bigdatatools.databricks.actions

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.ClusterInfoPresentable
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MainTreeController.Companion.dataManager

internal class DatabricksOpenClusterPageInBrowser  : DatabricksServerActionBase() {
  override fun performServerAction(dataManager: DatabricksDataManager, cluster: ClusterInfoPresentable) {
    var clusterUrl = dataManager.connectionData.getRealUri()
    clusterUrl = clusterUrl.substring(0, clusterUrl.length - (if(clusterUrl.endsWith("/")) 1 else 0))
    BrowserUtil.browse( "$clusterUrl/compute/clusters/${cluster.id}")
  }

  override fun isSkipConfirmation() = true
  override fun setShowConfirmation(isSelected: Boolean) = Unit
  override fun getConfirmationMessage(cluster: ClusterInfoPresentable) = ""

  override fun update(e: AnActionEvent) {
    val dataManager = e.dataManager as? DatabricksDataManager
    val cluster = dataManager?.getCurrentCluster()

    e.presentation.isEnabled = cluster != null
  }
}