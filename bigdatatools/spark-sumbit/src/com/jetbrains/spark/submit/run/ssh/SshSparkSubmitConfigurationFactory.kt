package com.jetbrains.spark.submit.run.ssh

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.spark.submit.run.SparkSubmitConfigurationType
import com.jetbrains.spark.submit.util.SparkMessagesBundle

class SshSparkSubmitConfigurationFactory(type: SparkSubmitConfigurationType) : ConfigurationFactory(type) {
  override fun getName(): String = SparkMessagesBundle.message("configuration.name.ssh")
  override fun getId(): String = Companion.id
  override fun createTemplateConfiguration(project: Project) = SshSparkJobRunConfiguration(project, this, name)

  companion object {
    const val id: String = "SshSparkJobConfigurationType"
  }

}