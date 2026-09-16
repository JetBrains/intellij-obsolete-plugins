package com.jetbrains.bigdatatools.dataproc.statistic

import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsCollector
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsEventGroup
import com.jetbrains.bigdatatools.dataproc.settings.DataprocSettingsCustomizer
import com.jetbrains.bigdatatools.gcloud.auth.GcloudAuthType

class DataprocSettingsCollector : BdtSettingsCollector() {
  override val connectionType: BdtConnectionType = BdtConnectionType.DATAPROC
  override val bdtGroup: BdtSettingsEventGroup = BdtSettingsEventGroup.METASTORE

  init {
    init()

    registryEvent(DataprocSettingsCustomizer::nameField)
    registryEvent(DataprocSettingsCustomizer::region)

    // Auth type
    registryStringEnumEvent(DataprocSettingsCustomizer::authTypeChooser,
                            GcloudAuthType.entries.filter { it != GcloudAuthType.ANON }.map { it.name })
    registryFieldAction(DataprocSettingsCustomizer::changeAccount)
    registryEvent(DataprocSettingsCustomizer::googleProject)
    registryEvent(DataprocSettingsCustomizer::credentialFileChooser)
  }

  object Util {
    fun getInstance(): DataprocSettingsCollector =
      getInstance(BdtConnectionType.DATAPROC) as? DataprocSettingsCollector ?: DataprocSettingsCollector()
  }
}