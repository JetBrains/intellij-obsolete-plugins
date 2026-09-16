package com.intellij.bigdatatools.databricks.run.configuration

import com.intellij.bigdatatools.databricks.icons.BigdatatoolsDatabricksIcons
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.execution.configurations.ConfigurationType
import javax.swing.Icon

internal class DatabricksRunConfigurationType : ConfigurationType {
  override fun getIcon(): Icon = BigdatatoolsDatabricksIcons.Databricks

  override fun getConfigurationTypeDescription(): String = DatabricksBundle.message("run.configuration.description")

  override fun getId(): String = DATABRICKS_ID

  override fun getDisplayName(): String = DatabricksBundle.message("run.configuration.name")

  override fun getConfigurationFactories() = arrayOf(DatabricksRunConfigurationFactory(this))

  companion object {
    const val DATABRICKS_ID = "DatabricksConfigurationType"
  }
}