package com.jetbrains.spark.submit.run.cluster.ui

import com.intellij.openapi.project.Project
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.ui.unified.SshUiData
import com.intellij.util.concurrency.annotations.RequiresEdt

object AttachedSshConfigsUtil {
  @RequiresEdt
  fun editAttachedSshConfigSetting(
    project: Project?,
    currentSelected: SshUiData,
    clusterName: String?,
    attachConfig: ((SshConfig) -> Unit)?,
  ): SshConfig? {
    error("This module will be deleted soon and must not be used")
  }
}