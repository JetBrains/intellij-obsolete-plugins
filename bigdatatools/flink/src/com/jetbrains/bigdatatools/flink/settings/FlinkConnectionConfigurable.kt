package com.jetbrains.bigdatatools.flink.settings

import com.intellij.bigdatatools.flink.icons.BigdatatoolsFlinkIcons
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.settings.RfsConnectionTestingBase
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionConfigurable
import com.jetbrains.bigdatatools.common.settings.defaultui.SettingsPanelCustomizerEx
import com.jetbrains.bigdatatools.flink.rfs.FlinkConnectionData

class FlinkConnectionConfigurable(connectionData: FlinkConnectionData, project: Project) :
  ConnectionConfigurable<FlinkConnectionData, FlinkSettingsCustomizer>(connectionData, project, BigdatatoolsFlinkIcons.Flink) {
  override fun getHelpTopic() = "big.data.tools.flink"
  override fun createSettingsCustomizer() = FlinkSettingsCustomizer(project, connectionData, disposable, coroutineScope)
  override fun createConnectionTesting() = FlinkConnectionDataRfsConnectionTestingBase(project, settingsCustomizer)
}

class FlinkConnectionDataRfsConnectionTestingBase(
  project: Project,
  settingsCustomizer: SettingsPanelCustomizerEx<FlinkConnectionData>?
) : RfsConnectionTestingBase<FlinkConnectionData>(project, settingsCustomizer)