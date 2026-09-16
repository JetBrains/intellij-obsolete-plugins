package com.intellij.bigdatatools.emr.statistic

import com.intellij.bigdatatools.awsBase.connection.auth.AuthenticationType
import com.intellij.bigdatatools.coreUi.connection.ProxyEnableType
import com.intellij.bigdatatools.emr.settings.EmrSettingsCustomizer
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsCollector
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsEventGroup

class EmrSettingsCollector : BdtSettingsCollector() {
  override val connectionType: BdtConnectionType = BdtConnectionType.EMR
  override val bdtGroup: BdtSettingsEventGroup = BdtSettingsEventGroup.METASTORE

  init {
    init()

    registryStringEnumEvent(EmrSettingsCustomizer::authTypeChooser, AuthenticationType.values.map { it.id })
    registryEvent(EmrSettingsCustomizer::region)
    registryEvent(EmrSettingsCustomizer::profileConfigPath)
    registryEvent(EmrSettingsCustomizer::profileCredentialsPath)
    registryEvent(EmrSettingsCustomizer::profileName)
    registryCheckboxEvent(EmrSettingsCustomizer::userCustomConfigPath)
    registryEvent(EmrSettingsCustomizer::nameField)
    registryEvent(EmrSettingsCustomizer::accessKey)
    registryEvent(EmrSettingsCustomizer::secretKey)

    registryEvent(EmrSettingsCustomizer::proxyDomainField)
    registryEvent(EmrSettingsCustomizer::proxyLoginField)
    registryEvent(EmrSettingsCustomizer::proxyPasswordField)
    registryEvent(EmrSettingsCustomizer::proxyHostField)
    registryEvent(EmrSettingsCustomizer::proxyPortField)
    registryEvent(EmrSettingsCustomizer::proxyWorkstation)
    registryEvent(EmrSettingsCustomizer::proxyNonProxyHostsField)
    registryStringEnumEvent(EmrSettingsCustomizer::proxyEnableComboBox, ProxyEnableType.entries.map { it.name })
    registryCheckboxEvent(EmrSettingsCustomizer::proxyBasicAuthCheckbox)
    registryEvent(EmrSettingsCustomizer::proxyIsDisabledProxy)
    registryEvent(EmrSettingsCustomizer::proxyAuthEnabledCheckbox)
  }

  object Util {
    fun getInstance(): EmrSettingsCollector = getInstance(BdtConnectionType.EMR) as? EmrSettingsCollector ?: EmrSettingsCollector()
  }
}
