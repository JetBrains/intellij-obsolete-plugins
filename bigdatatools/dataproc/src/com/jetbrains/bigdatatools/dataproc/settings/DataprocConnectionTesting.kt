package com.jetbrains.bigdatatools.dataproc.settings

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.driver.runInterruptibleMC
import com.jetbrains.bigdatatools.common.rfs.settings.RfsConnectionTestingBase
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionTestingSession
import com.jetbrains.bigdatatools.common.settings.defaultui.ConnectionStatus
import com.jetbrains.bigdatatools.common.settings.defaultui.ConnectionSuccessful
import com.jetbrains.bigdatatools.common.settings.defaultui.ConnectionWarning
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle

class DataprocConnectionTesting(project: Project, settingsCustomizer: DataprocSettingsCustomizer)
  : RfsConnectionTestingBase<DataprocConnectionData>(project, settingsCustomizer) {

  override suspend fun ConnectionTestingSession<DataprocConnectionData>.checkConnection(): ConnectionStatus {
    val clusters = runInterruptibleMC { DataprocConnectionChecker.checkConnection(testConnectionData) }
    if (clusters.isNotEmpty()) {
      return ConnectionSuccessful()
    }
    else {
      return ConnectionWarning(HdfsMessagesBundle.message("emr.connection.warning.no.clusters"),
                               HdfsMessagesBundle.message("emr.connection.warning.no.clusters.desc"))
    }
  }
}