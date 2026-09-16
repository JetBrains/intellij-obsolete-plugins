package com.intellij.bigdatatools.plugin.spark.arbitrary.wizard

import com.intellij.ssh.config.unified.SshConfig
import com.jetbrains.bigdatatools.sftp.settings.SftpConnectionData
import com.jetbrains.spark.monitoring.settings.SparkConnectionData

data class ArbitraryClusterWizardResult(
  val sshConfig: SshConfig,
  val spark: SparkConnectionData?,
  val sftp: SftpConnectionData?,
)