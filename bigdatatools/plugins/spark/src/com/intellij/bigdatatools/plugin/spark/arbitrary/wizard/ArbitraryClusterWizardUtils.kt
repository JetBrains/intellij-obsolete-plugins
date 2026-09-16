package com.intellij.bigdatatools.plugin.spark.arbitrary.wizard

import com.intellij.bigdatatools.plugin.spark.arbitrary.ArbitraryClusterConnectionGroup
import com.intellij.bigdatatools.plugin.spark.arbitrary.utils.ArbitraryClusterUtils
import com.intellij.execution.target.TargetEnvironmentWizard
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.use
import com.jetbrains.spark.submit.util.SparkMessagesBundle

object ArbitraryClusterWizardUtils {
  fun invokeWizard(project: Project): ArbitraryClusterWizardResult? {
    val initData = ArbitraryClusterConnectionGroup().createBlankData()
    Disposer.newDisposable().use { dispose ->
      val model = ArbitraryClusterTargetEnvConfiguration(project, initData, dispose)
      val steps = listOf(ArbitraryClusterTargetSshConnectionStep(model),
                         ArbitraryClusterTargetSparkConnectionStep(model),
                         ArbitraryClusterTargetSftpConnectionStep(model))
      val wizard = TargetEnvironmentWizard(project,
                                           SparkMessagesBundle.message("arbitrary.cluster.wizard.create.connection.title"),
                                           model,
                                           steps)

      if (!wizard.showAndGet())
        return null

      val sshConfig = model.selectedSshConfig.get() ?: return null


      val sparkConnectionData = when (model.selectedSparkType) {
        DependConnectionType.DEFAULT -> ArbitraryClusterUtils.createSparkConnection(model.connectionData)
        DependConnectionType.CUSTOM -> model.sparkConnectionData
        DependConnectionType.NONE -> null
      }

      val sftpConnectionData = when (model.selectedSftpType) {
        DependConnectionType.DEFAULT -> ArbitraryClusterUtils.createSftp(model.connectionData)
        DependConnectionType.CUSTOM -> model.sftpConnectionData
        DependConnectionType.NONE -> null
      }

      return ArbitraryClusterWizardResult(
        sshConfig,
        sparkConnectionData,
        sftpConnectionData,
      )
    }
  }
}

