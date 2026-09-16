package com.jetbrains.bigdatatools.dataproc.util

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import com.jetbrains.bigdatatools.dataproc.settings.DataprocToolWindowSettings
import com.jetbrains.bigdatatools.sftp.util.SshUtils

class DataprocDriverCreator(val dataManager: DataprocDataManager) {
  private var accessToken: String? = null
  private val cliManager = DataprocCliManager()


  fun getAccessToken(): String {
    accessToken?.let {
      return it
    }

    return cliManager.requestAccessToken()
  }

  fun createSshConsole(project: Project, cluster: DataprocClusterInfo, instanceName: String) {
    DataprocCliManager.runWithProgress(project, withModal = true) {
      val sshConfig = cliManager.getUpdatedOrCreateSshConfigSync(project, cluster.zone,
                                                                 dataManager.connectionData.projectId!!, instanceName)
      if (instanceName.endsWith(MASTER_SUFFIX))
        dataManager.dependsManager.attachConfig(instanceName.removeSuffix(MASTER_SUFFIX), sshConfig)
      SshUtils.runSshConsole(project, sshConfig, "/")
    }
  }

  fun updateSshConfig(project: Project?,
                      withModal: Boolean,
                      instanceName: String,
                      zone: String,
                      projectId: String) =
    DataprocCliManager.runWithProgress(project, withModal = withModal) {
      val config = cliManager.getUpdatedOrCreateSshConfigSync(project, zone = zone, instanceName = instanceName, projectId = projectId)
      if (instanceName.endsWith(MASTER_SUFFIX))
        dataManager.dependsManager.attachConfig(instanceName.removeSuffix(MASTER_SUFFIX), config)
      config
    }


  fun revalidateSshKeysIfRequired(instanceName: String, zone: String): Boolean = synchronized(syncSshUpdateObj) {
    val currentTimeMillis = System.currentTimeMillis()
    if (currentTimeMillis - DataprocToolWindowSettings.getInstance().lastTimeUpdateKey < EXPIRE_TIME_MILLS) {
      return@synchronized false
    }

    cliManager.generateSshKeys(instanceName = instanceName, zone = zone, projectId = dataManager.connectionData.projectId ?: "")
    DataprocToolWindowSettings.getInstance().lastTimeUpdateKey = currentTimeMillis
    return@synchronized true
  }


  companion object {
    const val MASTER_SUFFIX = "-m"
    val syncSshUpdateObj = Any()
    const val EXPIRE_TIME_MILLS = 1000 * 60 * 60
  }
}