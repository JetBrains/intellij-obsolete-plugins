package com.intellij.bigdatatools.databricks.actions

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.ClusterInfoPresentable
import com.intellij.bigdatatools.databricks.toolwindow.config.DatabricksToolWindowSettings
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MainTreeController.Companion.dataManager
import org.jetbrains.annotations.Nls

internal class DatabricksStopServerAction : DatabricksServerActionBase() {
  override fun isSkipConfirmation() = DatabricksToolWindowSettings.getInstance().skipStopServerConfirmation

  override fun setShowConfirmation(isSelected: Boolean) {
    DatabricksToolWindowSettings.getInstance().skipStopServerConfirmation = isSelected
  }

  override fun getConfirmationMessage(cluster: ClusterInfoPresentable): @Nls String {
    return DatabricksBundle.message("stop.cluster.confirmation", cluster.name)
  }

  override fun performServerAction(dataManager: DatabricksDataManager, cluster: ClusterInfoPresentable) {
    dataManager.stopCluster(cluster)
  }

  override fun update(e: AnActionEvent) {
    val dataManager = e.dataManager as? DatabricksDataManager
    val cluster = dataManager?.getCurrentCluster()
    e.presentation.isEnabledAndVisible = cluster?.isClusterReady() == true
  }
}