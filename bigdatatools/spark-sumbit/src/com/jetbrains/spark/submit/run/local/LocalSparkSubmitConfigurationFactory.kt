package com.jetbrains.spark.submit.run.local

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.intellij.util.EnvironmentUtil
import com.jetbrains.spark.submit.run.SparkSubmitConfigurationType
import com.jetbrains.spark.submit.util.SparkMessagesBundle

class LocalSparkSubmitConfigurationFactory(type: SparkSubmitConfigurationType) : ConfigurationFactory(type) {
  override fun getName(): String = SparkMessagesBundle.message("configuration.name.local")

  override fun getId(): String = ID

  override fun createTemplateConfiguration(project: Project): LocalSparkJobRunConfiguration {
    val sparkHome = EnvironmentUtil.getValue("SPARK_HOME") ?: ""
    val interpreterPath = EnvironmentUtil.getValue("SHELL") ?: "/bin/sh"

    val localSparkJobRunConfiguration = LocalSparkJobRunConfiguration(project, this, name)
    localSparkJobRunConfiguration.sparkHome = sparkHome
    localSparkJobRunConfiguration.shellExecutor = interpreterPath
    return localSparkJobRunConfiguration
  }

  companion object {
    const val ID: String = "SparkJobConfigurationType"
  }
}