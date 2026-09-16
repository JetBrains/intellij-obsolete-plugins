package com.intellij.bigdatatools.emr.settings

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.driver.runInterruptibleMC
import com.jetbrains.bigdatatools.common.rfs.settings.RfsConnectionTestingBase
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionTestingSession
import com.jetbrains.bigdatatools.common.settings.defaultui.ConnectionStatus
import com.jetbrains.bigdatatools.common.settings.defaultui.ConnectionSuccessful
import com.jetbrains.bigdatatools.common.settings.defaultui.ConnectionWarning
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle

class EmrTesting(project: Project,
                 settingsCustomizer: EmrSettingsCustomizer) : RfsConnectionTestingBase<EmrConnectionData>(project,
                                                                                                          settingsCustomizer) {
  override suspend fun ConnectionTestingSession<EmrConnectionData>.checkConnection(): ConnectionStatus {
    val clusters = runInterruptibleMC { EmrConnectionChecker.checkConnection(testConnectionData) }
    if (clusters.isNotEmpty()) {
      return ConnectionSuccessful()
    }
    else {
      return ConnectionWarning(HdfsMessagesBundle.message("emr.connection.warning.no.clusters"),
                               HdfsMessagesBundle.message("emr.connection.warning.no.clusters.desc"))
    }
  }
}