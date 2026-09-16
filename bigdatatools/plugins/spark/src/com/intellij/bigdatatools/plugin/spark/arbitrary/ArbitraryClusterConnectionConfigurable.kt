package com.intellij.bigdatatools.plugin.spark.arbitrary

import com.intellij.bigdatatools.plugin.spark.BigdatatoolsPluginSparkIcons
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionConfigurable

class ArbitraryClusterConnectionConfigurable(connectionData: ArbitraryClusterConnectionData, project: Project) :
  ConnectionConfigurable<ArbitraryClusterConnectionData, ArbitraryClusterSettingsCustomizer>(connectionData, project,
                                                                                             BigdatatoolsPluginSparkIcons.ArbitraryCluster) {

  override fun createSettingsCustomizer() = ArbitraryClusterSettingsCustomizer(project, connectionData, disposable)

  override fun isInitWithWizard(): Boolean = true
}