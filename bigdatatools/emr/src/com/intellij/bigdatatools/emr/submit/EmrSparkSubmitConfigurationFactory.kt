package com.intellij.bigdatatools.emr.submit

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.UnknownConfigurationType
import com.intellij.openapi.project.Project
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle

object EmrSparkSubmitConfigurationFactory : ConfigurationFactory(UnknownConfigurationType.getInstance()) {
  override fun getName(): String = HdfsMessagesBundle.message("emr.spark.submit")
  override fun getId(): String = "EmrSparkJobConfigurationType"
  override fun createTemplateConfiguration(project: Project) = EmrSparkJobRunConfiguration(project, this, name)
}