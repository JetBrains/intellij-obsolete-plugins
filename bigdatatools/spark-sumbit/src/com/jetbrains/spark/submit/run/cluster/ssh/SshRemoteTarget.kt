package com.jetbrains.spark.submit.run.cluster.ssh

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.execution.process.ProcessHandler
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.EDT
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.config.unified.SshConfigManager
import com.intellij.ssh.ui.unified.SshUiData
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.jetbrains.spark.submit.run.cluster.RemoteTarget
import com.jetbrains.spark.submit.run.cluster.RemoteTargetId
import com.jetbrains.spark.submit.run.cluster.RemoteTargetType
import com.jetbrains.spark.submit.run.cluster.ui.AttachedSshConfigsUtil
import com.jetbrains.spark.submit.run.cluster.ui.ClusterSparkSubmitConfigurationEditor
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.Icon

class SshRemoteTarget(val project: Project, name: String, private val sshConfigId: String) : RemoteTarget(name, createId(sshConfigId, name)) {
  override val icon: Icon = AllIcons.Debugger.Console

  override val type: RemoteTargetType = RemoteTargetType.SSH

  override fun setupItem(item: SimpleColoredComponent) {
    item.icon = this@SshRemoteTarget.icon
    item.append(this@SshRemoteTarget.name)
    item.append("  ")
    item.append(SparkMessagesBundle.message("remote.target.ssh.remark"), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
  }

  override suspend fun getOrDefaultInitSshConfig(project: Project): SshConfig {
    return SshConfigManager.getInstance(project).findConfigById(sshConfigId) ?: throw ConfigurationException(
      SparkMessagesBundle.message("error.ssh"))
  }

  override suspend fun openSshConfigDialog(editor: ClusterSparkSubmitConfigurationEditor): SshConfig? {
    val currentUiData = SshUiData.create(getOrDefaultInitSshConfig(project))
    return withContext(Dispatchers.EDT) {
      val newSelected = AttachedSshConfigsUtil.editAttachedSshConfigSetting(project, currentUiData, null, null)
      if (currentUiData.config != newSelected && newSelected != null) {
        editor.sshRowVisible.set(false)
        editor.clusterComboBox.loadValues(createId(newSelected))
      }
      return@withContext null
    }
  }

  override fun addApplicationToMonitoring(processHandler: ProcessHandler, name: String, focusOnApp: Boolean) {}

  override val sshDetailsLinkCaption: String
    get() = MessagesBundle.message("ssh.open.settings")

  companion object {
    const val FAKE_CONNECTION_ID = "ssh"
    fun createId(sshConfigId: String, name: String) = RemoteTargetId(FAKE_CONNECTION_ID, sshConfigId, name)
    fun createId(sshConfig: SshConfig) = createId(sshConfig.id, sshConfig.presentableShortName)
  }

}