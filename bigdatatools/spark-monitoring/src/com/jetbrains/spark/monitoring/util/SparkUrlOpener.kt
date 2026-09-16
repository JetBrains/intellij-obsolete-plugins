package com.jetbrains.spark.monitoring.util

import com.intellij.ide.BrowserUtil
import com.jetbrains.bigdatatools.common.rfs.driver.SafeExecutor
import com.jetbrains.spark.monitoring.models.SparkDataManager

object SparkUrlOpener {
  fun openUrl(url: String, dataManager: SparkDataManager) = SafeExecutor.instance.asyncSuspend(
    SMMessagesBundle.message("process.open.logs")) {
    val tunnelData = dataManager.connectionData.getTunnelData()
    if (!tunnelData.isEnabled) {
      BrowserUtil.open(url)
      return@asyncSuspend
    }
    val (finalUrl, _) = dataManager.client.restClient.performGetWithTunnel(url, dataManager.project, tunnelData)
    BrowserUtil.open(finalUrl)
  }
}