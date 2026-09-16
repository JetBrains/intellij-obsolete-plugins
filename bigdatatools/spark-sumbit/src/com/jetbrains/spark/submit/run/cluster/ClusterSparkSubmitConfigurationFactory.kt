package com.jetbrains.spark.submit.run.cluster

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.spark.submit.run.SparkSubmitConfigurationType
import com.jetbrains.spark.submit.util.SparkMessagesBundle

class ClusterSparkSubmitConfigurationFactory(type: SparkSubmitConfigurationType) : ConfigurationFactory(type) {
  val isPySpark = type.isPySpark
  override fun getName(): String = SparkMessagesBundle.message("configuration.name.cluster")
  override fun getId(): String = Companion.id
  override fun createTemplateConfiguration(project: Project) = ClusterSparkJobRunConfiguration(project, this, name, isTemplate = true)
  override fun createConfiguration(name: @NlsSafe String?, template: RunConfiguration): RunConfiguration {
    return super.createConfiguration(name, template).also {
      (it as ClusterSparkJobRunConfiguration).isTemplate = false
    }
  }

  companion object {
    const val id: String = "ClusterSparkJobConfigurationType"
  }

}