package com.intellij.bigdatatools.plugin.spark.services.actions

import com.intellij.bigdatatools.plugin.spark.arbitrary.ArbitraryClusterConnectionData
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.common.rfs.driver.depend.MasterConnectionData
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.bigdatatools.common.util.ConnectionUtil
import com.jetbrains.spark.monitoring.settings.SparkConnectionData

class EnableServiceConnection : ServiceConnectionAction() {
  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    if (!isRootDriver(e)) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    val selectedConnectionIds = getSelectedConnectionIds(e)
    val selectedAndDisabledNodes = selectedConnectionIds.filter {
      RfsConnectionDataManager.instance?.getConnectionById(project, it)?.isEnabled == false
    }

    e.presentation.isEnabledAndVisible = selectedAndDisabledNodes.isNotEmpty()
    e.presentation.text = MessagesBundle.message("rfs.action.enable.connection", if (selectedAndDisabledNodes.size == 1) 0 else 1)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val selectedConnectionIds = getSelectedConnection(e)
    if (selectedConnectionIds.isEmpty()) return

    val (arbitrary, other) = selectedConnectionIds.partition { it is ArbitraryClusterConnectionData }

    val additionalSparks = arbitrary.mapNotNull { connData ->
      (connData as MasterConnectionData<*>)
        .getDependConnections(project)
        .firstOrNull { it is SparkConnectionData }
    }

    ConnectionUtil.enableConnectionsByData(project, (arbitrary + other + additionalSparks).distinct())
  }
}