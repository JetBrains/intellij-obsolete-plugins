package com.jetbrains.spark.submit.run.ssh.util

import com.intellij.openapi.project.Project
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.ui.unified.SshUiData
import com.jetbrains.plugins.webDeployment.config.FileTransferConfig
import com.jetbrains.plugins.webDeployment.config.ServerPasswordSafeDeployable
import com.jetbrains.plugins.webDeployment.config.WebServerConfig
import com.jetbrains.plugins.webDeployment.ui.ServerBrowserDialog
import org.jetbrains.annotations.Nls

object SshSparkEditorUtils {

  fun selectSingleHostFile(project: Project,
                           conf: SshConfig,
                           @Nls(capitalization = Nls.Capitalization.Title) dialogTitle: String,
                           oldPath: String? = null): String? {
    val sshUiData = SshUiData.create(conf)
    val wsConfig = WebServerConfig()
    wsConfig.initializeNewCreatedServer(false)
    val safeDeployable = ServerPasswordSafeDeployable(wsConfig, sshUiData)
    val dialog = ServerBrowserDialog(
      project,
      safeDeployable,
      dialogTitle,
      false,
      FileTransferConfig.Origin.Default,
      WebServerConfig.RemotePath(oldPath)
    )
    val isChosen = dialog.showAndGet()
    if (!isChosen) return null

    return dialog.path?.path ?: return null
  }
}