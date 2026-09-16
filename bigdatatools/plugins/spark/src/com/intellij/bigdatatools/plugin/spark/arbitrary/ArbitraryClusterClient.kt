package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.ssh.config.unified.SshConfigManager
import com.intellij.ssh.ui.unified.SshUiData
import com.intellij.bigdatatools.coreUi.connection.exception.BdtConfigurationException
import com.jetbrains.bigdatatools.common.monitoring.connection.MonitoringClient
import com.jetbrains.bigdatatools.common.util.BdtSshUtils

class ArbitraryClusterClient(project: Project?, val connectionData: ArbitraryClusterConnectionData) : MonitoringClient(project) {
  override fun getRealUri() = connectionData.innerId

  override fun checkConnectionInner() {
    val sshId = connectionData.sshId
    if (sshId.isBlank())
      throw BdtConfigurationException("SSH config is not setup")

    val config = SshConfigManager.getInstance(project).findConfigById(sshId) ?: throw BdtConfigurationException("SSH config is not setup")

    try {
      runBlockingMaybeCancellable {
        BdtSshUtils.testConnectionOrThrow(SshUiData.create(config), project, withValidation = false)
      }
    }
    catch (t: Throwable) {
      throw BdtConfigurationException("Cannot connect through SSH", t)
    }
  }

  override fun connectInner(calledByUser: Boolean) {
  }

  override fun dispose() {

  }
}