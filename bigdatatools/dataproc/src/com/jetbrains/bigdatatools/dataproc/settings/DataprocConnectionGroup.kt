package com.jetbrains.bigdatatools.dataproc.settings

import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionFactory
import com.intellij.bigdatatools.dataproc.icons.BigdatatoolsDataprocIcons
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.settings.connections.DataProcessingPlatformConnectionGroup
import com.jetbrains.bigdatatools.gcloud.auth.GcloudAuthType

class DataprocConnectionGroup : ConnectionFactory<DataprocConnectionData>(
  id = BdtConnectionType.DATAPROC.id,
  name = BdtConnectionType.DATAPROC.connName,
  icon = BigdatatoolsDataprocIcons.Dataproc,
  parentGroupId = DataProcessingPlatformConnectionGroup.GROUP_ID) {

  override fun newData(): DataprocConnectionData = DataprocConnectionData().apply {
    authType = GcloudAuthType.ACCOUNT.id
    name = BdtConnectionType.DATAPROC.connName
  }

}

