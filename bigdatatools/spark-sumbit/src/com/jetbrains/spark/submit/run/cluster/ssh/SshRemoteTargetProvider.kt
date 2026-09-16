package com.jetbrains.spark.submit.run.cluster.ssh

import com.intellij.openapi.project.Project
import com.intellij.ssh.config.unified.SshConfigManager
import com.jetbrains.bigdatatools.common.rfs.driver.depend.MasterConnectionData
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.spark.submit.run.cluster.RemoteTargetId
import com.jetbrains.spark.submit.run.cluster.RemoteTargetProvider

class SshRemoteTargetProvider : RemoteTargetProvider {
  override suspend fun getTargetById(project: Project, id: RemoteTargetId): SshRemoteTarget? {
    if (id.connectionId != SshRemoteTarget.FAKE_CONNECTION_ID)
      return null

    val sshConfig = SshConfigManager.getInstance(project).findConfigById(id.clusterId) ?: return null
    return SshRemoteTarget(project, sshConfig.presentableShortName, sshConfig.id)
  }

  override suspend fun getRemoteTargets(project: Project): List<SshRemoteTarget> {
    val configs = SshConfigManager.getInstance(project).configs

    val masterConnections = RfsConnectionDataManager.instance?.getConnections(project)?.filterIsInstance<MasterConnectionData<*>>()
                            ?: emptyList()

    val dependSshConfigs = masterConnections.flatMap { it.sshConfigsByClusterId().values }.toSet()

    val independentConfigs = configs.filter { it.id !in dependSshConfigs }
    return independentConfigs.map { SshRemoteTarget(project, it.presentableShortName, it.id) }
  }

  override fun createConnectionTarget(project: Project) = null
}