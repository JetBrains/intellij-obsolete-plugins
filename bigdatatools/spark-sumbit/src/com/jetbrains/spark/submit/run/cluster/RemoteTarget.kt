package com.jetbrains.spark.submit.run.cluster

import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.jetbrains.spark.submit.run.cluster.ui.ClusterSparkSubmitConfigurationEditor

abstract class RemoteTarget(name: String, id: RemoteTargetId) : SelectableRemoteTargetOption(name, id) {

  abstract suspend fun getOrDefaultInitSshConfig(project: Project): SshConfig

  @RequiresEdt
  open fun updateEditorFields(editor: ClusterSparkSubmitConfigurationEditor) {

  }

  abstract suspend fun openSshConfigDialog(editor: ClusterSparkSubmitConfigurationEditor): SshConfig?

  @get:NlsContexts.Button
  abstract val sshDetailsLinkCaption: String

  //abstract fun openSparkUrl(project: Project, url: String, configuration: SshAwareSparkJobRunConfiguration)

  //abstract suspend fun waitAndSuggestToOpenMonitoring(project: Project, url: String, configuration: SshAwareSparkJobRunConfiguration)

  @RequiresBackgroundThread
  abstract fun addApplicationToMonitoring(processHandler: ProcessHandler, name: String, focusOnApp: Boolean)

  @RequiresBackgroundThread
  open fun getOrCreateSparkConnection(): ConnectionData? = null

}


