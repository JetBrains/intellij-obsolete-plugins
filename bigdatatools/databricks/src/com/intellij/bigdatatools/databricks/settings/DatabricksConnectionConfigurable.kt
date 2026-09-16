package com.intellij.bigdatatools.databricks.settings

import com.intellij.bigdatatools.databricks.icons.BigdatatoolsDatabricksIcons
import com.intellij.bigdatatools.databricks.rfs.DatabricksConnectionData
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionConfigurable
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionTesting

class DatabricksConnectionConfigurable(connectionData: DatabricksConnectionData, project: Project) :
  ConnectionConfigurable<DatabricksConnectionData, DatabricksSettingsCustomizer>(connectionData, project, BigdatatoolsDatabricksIcons.Databricks) {
  override fun getHelpTopic() = "big.data.tools.databricks"
  override fun createSettingsCustomizer() = DatabricksSettingsCustomizer(project, connectionData, disposable, coroutineScope)
  override fun createConnectionTesting(): ConnectionTesting<DatabricksConnectionData> =
    DatabricksConnectionTesting(project, settingsCustomizer)
}