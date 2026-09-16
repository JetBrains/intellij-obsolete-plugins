package com.jetbrains.bigdatatools.dataproc.util

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.remote.AuthType
import com.intellij.remote.SshConnectionConfigPatch
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.config.unified.SshConfigManager
import com.intellij.ssh.getHomeSshDirectory
import com.intellij.ssh.ui.unified.SshUiData
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.jetbrains.bigdatatools.gcloud.sdk.GCloudSdkService
import java.io.File
import java.io.FileNotFoundException
import java.time.Duration

class DataprocCliManager {
  fun getUpdatedOrCreateSshConfigSync(project: Project?,
                                      zone: String,
                                      projectId: String,
                                      instanceName: String) = getUpdatedSshConfig(instanceName = instanceName,
                                                                                  project = project,
                                                                                  zone = zone,
                                                                                  projectId = projectId) {}

  private fun getUpdatedSshConfig(instanceName: String,
                                  project: Project?,
                                  zone: String,
                                  projectId: String,
                                  callback: (SshConfig) -> Unit): SshConfig {
    val sshUiData = createSshConfig(instanceName, zone, projectId, project, callback).let {
      SshUiData.create(it, true)
    }
    val registered = SshConfigManager.getInstance(project).register(sshUiData)

    registered.config.connectionConfigPatch = SshConnectionConfigPatch(hostKeyVerifier = null,
                                                                       serverAliveInterval = Duration.ofSeconds(5),
                                                                       proxyParams = null)
    callback(registered.config)
    return registered.config
  }

  private fun createSshConfig(instanceName: String,
                              zone: String,
                              projectId: String,
                              project: Project?,
                              callback: (SshConfig) -> Unit): SshConfig {
    val sshName = "dataproc-$instanceName"

    val sshInit = getSshInitInfo(instanceName = instanceName, zone = zone, projectId = projectId)

    SshConfigManager.getInstance(project).findConfigByName(sshName)?.let {
      updateConfig(it, sshInit)
      callback(it)
      return it
    }


    val sshConfig = SshConfig(true).apply {
      this.setHost(sshInit.host)
      this.setKeyPath(sshInit.keyPath.absolutePath)
      this.port = 22
      this.authType = AuthType.KEY_PAIR

      this.customName = sshName
      this.setUsername(sshInit.username)
    }

    return sshConfig
  }

  private fun updateConfig(config: SshConfig, sshInit: SshInitResult) {
    config.setHost(sshInit.host)
    config.setUsername(sshInit.username)
    config.setKeyPath(sshInit.keyPath.absolutePath)
  }


  private fun getSshInitInfo(instanceName: String, zone: String, projectId: String): SshInitResult {
    val gCloudSdkService = GCloudSdkService.getInstance()

    val keyPath = getHomeSshDirectory().resolve("google_compute_engine")
    val command = listOf("compute", "ssh", instanceName, "--zone=$zone", "--project=$projectId")
    val result = gCloudSdkService.executeCommand(command + "--dry-run", false)

    val (name, host) = result.trim().split(" ").last().split("@")
    val file = keyPath.toFile()
    if (!file.exists())
      throw FileNotFoundException(file.absolutePath)

    return SshInitResult(file, username = name, host = host)
  }

  fun generateSshKeys(instanceName: String, zone: String, projectId: String) {
    val gCloudSdkService = GCloudSdkService.getInstance()
    val command = listOf("compute", "ssh", instanceName, "--zone=$zone", "--project=$projectId")
    gCloudSdkService.executeCommand(command + "--command=\"ls\"", false)
  }

  fun requestAccessToken(): @NlsSafe String {
    val command = listOf("auth", "print-access-token")
    val gCloudSdkService = GCloudSdkService.getInstance()
    val result = gCloudSdkService.executeCommand(command)
    val stringMap = BdtJson.fromJsonToClassMap(result, String::class.java)
    return stringMap["token"]!!
  }

  data class SshInitResult(val keyPath: File, val username: String, val host: String)

  companion object {
    @RequiresBackgroundThread
    fun <T> runWithProgress(project: Project?, withModal: Boolean, body: (ProgressIndicator?) -> T): T {
      fun f(indicator: ProgressIndicator?): T {
        indicator?.isIndeterminate = true
        indicator?.text = DataprocMessagesBundle.message("task.init.ssh.perform.cli.command")

        return body(indicator)
      }

      return if (withModal) {
        val task = object : Task.WithResult<T, Exception>(project, DataprocMessagesBundle.message("task.init.ssh.title"), false) {
          override fun compute(indicator: ProgressIndicator): T = f(indicator)
        }
        ProgressManager.getInstance().run(task)
      }
      else {
        f(null)
      }
    }
  }
}