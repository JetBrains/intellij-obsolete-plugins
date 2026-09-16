package com.intellij.bigdatatools.databricks.run.configuration

import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project

internal class DatabricksRunConfigurationFactory(type: DatabricksRunConfigurationType) : ConfigurationFactory(type) {
  override fun getName(): String = DatabricksBundle.message("run.configuration.name")

  override fun getId(): String = ID

  override fun createTemplateConfiguration(project: Project): DatabricksRunConfiguration {
    return DatabricksRunConfiguration(project, this, name)
  }

  companion object {
    const val ID: String = "DatabricksRunConfigurationFactory"
  }
}