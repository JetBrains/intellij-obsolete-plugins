package com.intellij.bigdatatools.emr.dependend

import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.bigdatatools.emr.model.EmrClusterDetails
import com.intellij.bigdatatools.emr.settings.EmrSshKeysStorage
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.remote.AuthType
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.config.unified.SshConfigManager
import com.intellij.ssh.ui.unified.SshUiData
import com.jetbrains.spark.submit.run.cluster.ui.AttachedSshConfigsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmrSshManager(val dataManager: EmrDataManager) {
  val connectionData = dataManager.connectionData

  suspend fun getOrAskSetupOrShowError(project: Project?, clusterInfo: EmrClusterDetails) =
    getOrAskSetup(project, clusterInfo) ?: let {
      withContext(Dispatchers.EDT) {
        Messages.showErrorDialog(project,
                                 EmrMessagesBundle.message("ssh.tunnel.is.not.setup"),
                                 EmrMessagesBundle.message("error.title"))

      }
      null
    }

  private suspend fun getOrAskSetup(project: Project?, clusterInfo: EmrClusterDetails): SshConfig? {
    getCachedSsh(project, clusterInfo)?.let {
      return it
    }

    return chooseFromUiConnectionData(project, clusterInfo)
  }

  private fun getCachedSsh(project: Project?, clusterInfo: EmrClusterDetails): SshConfig? {
    val cluster = clusterInfo.cluster
    val masterPublicDnsName = cluster.masterPublicDnsName()
    var configId = connectionData.sshConfigs[cluster.id()]

    val storedSshConfigs = SshConfigManager.getInstance(project).configs

    //Migrate from old version
    if (configId == null) {
      configId = storedSshConfigs.firstOrNull { it.host == masterPublicDnsName }?.id
      configId?.let { connectionData.sshConfigs[cluster.id()] = configId }
    }

    return storedSshConfigs.firstOrNull { configId != null && it.id == configId }
  }

  suspend fun chooseFromUiConnectionData(project: Project?, cluster: EmrClusterDetails): SshConfig? {
    val currentUiData = SshUiData.create(getOrCreateSshConfig(project, cluster))
    return withContext(Dispatchers.EDT) {
      val chosenConfig = AttachedSshConfigsUtil.editAttachedSshConfigSetting(project, currentUiData, cluster.cluster.name()) {
        attachConfig(cluster, it)
      } ?: return@withContext null
      return@withContext chosenConfig
    }
  }

  private fun saveSshConfig(project: Project?,
                            newData: SshUiData,
                            cluster: EmrClusterDetails): SshUiData {
    val conf = SshConfigManager.getInstance(project).register(newData)
    val key = cluster.sshKeyName
    if (key != null) {
      EmrSshKeysStorage.getInstance().put(key, newData.privateKeyFile)
    }
    return conf
  }


  private fun attachConfig(clusterInfo: EmrClusterDetails, sshConfig: SshConfig) {
    val cluster = clusterInfo.cluster
    connectionData.sshConfigs[cluster.id()] = sshConfig.id
  }


  fun getOrCreateSshConfig(project: Project?, cluster: EmrClusterDetails): SshConfig {
    getCachedSsh(project, cluster)?.let { return it }
    val newConfig = createTemplateSshConfig(cluster)
    val saved = SshConfigManager.getInstance(project).register(SshUiData.create(newConfig))
    saveSshConfig(project, saved, cluster)
    attachConfig(cluster, saved.config)
    return saved.config
  }

  private fun createTemplateSshConfig(cluster: EmrClusterDetails): SshConfig {
    val key = cluster.sshKeyName
    val pemFile = EmrSshKeysStorage.getInstance().getPathForKey(key)

    return SshConfig(true).apply {
      this.setHost(cluster.cluster.masterPublicDnsName()?.takeIf { it.isNotBlank() } ?: "")
      this.setKeyPath(pemFile)
      this.port = 22
      this.authType = AuthType.KEY_PAIR
      this.setUsername("hadoop")
      this.customName = "EMR: ${cluster.cluster.name()}"
    }
  }
}