package com.intellij.bigdatatools.zeppelin.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionGroup
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.constants.BdtPluginType
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionConfigurable
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionSettingProvider
import com.jetbrains.bigdatatools.common.settings.connections.NotebookConnectionGroup

class ZeppelinSettingsProvider : ConnectionSettingProvider {
  override val pluginType: BdtPluginType = BdtPluginType.ZEPPELIN

  override fun createConnectionGroups(): List<ConnectionGroup> = listOf(ZeppelinConnectionGroup())

}

class ZeppelinConnectionGroup : ConnectionFactory<ZeppelinConnectionData>(
  id = BdtConnectionType.ZEPPELIN.id,
  name = BdtConnectionType.ZEPPELIN.connName,
  icon = ZeppelinDriver.driverIcon,
  parentGroupId = NotebookConnectionGroup.GROUP_ID
) {
  override fun newData(): ZeppelinConnectionData = ZeppelinConnectionData().apply {
    name = BdtConnectionType.ZEPPELIN.connName
    uri = "localhost:8080"
    isZtoolsEnabled = true
  }
}

class ZeppelinConnectionConfigurable(
  connectionData: ZeppelinConnectionData,
  project: Project
) : ConnectionConfigurable<ZeppelinConnectionData, ZeppelinSettingsCustomizer>(connectionData, project, ZeppelinDriver.driverIcon) {
  override fun createSettingsCustomizer() = ZeppelinSettingsCustomizer(project, connectionData, disposable, coroutineScope)
  override fun createConnectionTesting() = ZeppelinTestingBase(project, settingsCustomizer)
}

