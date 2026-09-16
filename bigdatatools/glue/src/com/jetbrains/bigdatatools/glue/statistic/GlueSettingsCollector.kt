package com.jetbrains.bigdatatools.glue.statistic

import com.intellij.bigdatatools.awsBase.connection.auth.AuthenticationType
import com.intellij.bigdatatools.coreUi.connection.ProxyEnableType
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsCollector
import com.jetbrains.bigdatatools.common.rfs.statistics.v2.BdtSettingsEventGroup
import com.jetbrains.bigdatatools.glue.settings.GlueSettingsCustomizer

class GlueSettingsCollector : BdtSettingsCollector() {
  override val connectionType: BdtConnectionType = BdtConnectionType.GLUE
  override val bdtGroup: BdtSettingsEventGroup = BdtSettingsEventGroup.METASTORE

  init {
    init()

    registryStringEnumEvent(GlueSettingsCustomizer::authTypeChooser, AuthenticationType.values.map { it.id })
    registryEvent(GlueSettingsCustomizer::region)
    registryEvent(GlueSettingsCustomizer::profileConfigPath)
    registryEvent(GlueSettingsCustomizer::profileCredentialsPath)
    registryEvent(GlueSettingsCustomizer::profileName)
    registryCheckboxEvent(GlueSettingsCustomizer::userCustomConfigPath)
    registryEvent(GlueSettingsCustomizer::nameField)
    registryEvent(GlueSettingsCustomizer::accessKey)
    registryEvent(GlueSettingsCustomizer::secretKey)

    registryEvent(GlueSettingsCustomizer::proxyDomainField)
    registryEvent(GlueSettingsCustomizer::proxyLoginField)
    registryEvent(GlueSettingsCustomizer::proxyPasswordField)
    registryEvent(GlueSettingsCustomizer::proxyHostField)
    registryEvent(GlueSettingsCustomizer::proxyPortField)
    registryEvent(GlueSettingsCustomizer::proxyWorkstation)
    registryEvent(GlueSettingsCustomizer::proxyNonProxyHostsField)
    registryStringEnumEvent(GlueSettingsCustomizer::proxyEnableComboBox, ProxyEnableType.entries.map { it.name })
    registryCheckboxEvent(GlueSettingsCustomizer::proxyBasicAuthCheckbox)
    registryEvent(GlueSettingsCustomizer::proxyIsDisabledProxy)
    registryEvent(GlueSettingsCustomizer::proxyAuthEnabledCheckbox)
  }

  object Util {
    fun getInstance(): GlueSettingsCollector = getInstance(BdtConnectionType.GLUE) as? GlueSettingsCollector ?: GlueSettingsCollector()
  }
}
