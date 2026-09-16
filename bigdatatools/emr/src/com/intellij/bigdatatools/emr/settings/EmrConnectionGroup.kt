package com.intellij.bigdatatools.emr.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.settings.connections.DataProcessingPlatformConnectionGroup
import org.com.jetbrains.bigdatatools.icons.Icons

class EmrConnectionGroup : ConnectionFactory<EmrConnectionData>(
  id = BdtConnectionType.EMR.id,
  name = BdtConnectionType.EMR.connName,
  icon = Icons.EMR_ICON,
  parentGroupId = DataProcessingPlatformConnectionGroup.GROUP_ID
) {
  override fun newData() = EmrConnectionData().apply {
    name = BdtConnectionType.EMR.connName
  }

  companion object {
    val ICON = Icons.EMR_ICON
  }
}

