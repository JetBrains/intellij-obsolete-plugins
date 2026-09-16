package com.jetbrains.spark.monitoring.settings

import com.intellij.bigdatatools.coreUi.connection.ProxyEnableType
import com.intellij.bigdatatools.coreUi.connection.ProxyType
import com.intellij.bigdatatools.coreUi.fields.CheckBoxField
import com.intellij.bigdatatools.coreUi.fields.ComboBoxField
import com.intellij.bigdatatools.coreUi.fields.CredentialsHolder
import com.intellij.bigdatatools.coreUi.fields.IntNamedField
import com.intellij.bigdatatools.coreUi.fields.PasswordNamedField
import com.intellij.bigdatatools.coreUi.fields.StringNamedField
import com.intellij.bigdatatools.coreUi.fields.StringNonRequiredField
import com.intellij.bigdatatools.coreUi.fields.UsernameNamedField
import com.intellij.bigdatatools.coreUi.settings.CommonSettingsKeys
import com.intellij.bigdatatools.coreUi.settings.withNotEmptyValidator
import com.intellij.bigdatatools.coreUi.settings.withNumberValidatorOrEmpty
import com.intellij.bigdatatools.coreUi.settings.withPortValidator
import com.intellij.bigdatatools.coreUi.settings.withProxyHostValidator
import com.intellij.bigdatatools.coreUi.util.BdIdeRegistryUtil
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.HttpSettingsCustomizer
import com.jetbrains.spark.monitoring.statistics.SparkMonitoringSettingsCollector
import kotlinx.coroutines.CoroutineScope

class SparkSettingsCustomizer(
  project: Project,
  connectionData: SparkConnectionData,
  uiDisposable: Disposable,
  coroutineScope: CoroutineScope
) :
  HttpSettingsCustomizer<SparkConnectionData>(uiDisposable, project, connectionData) {

  override val operationTimeout: StringNonRequiredField<SparkConnectionData> = StringNonRequiredField(
    SparkConnectionData::operationTimeout,
    CommonSettingsKeys.OPERATIONS_TIMEOUT,
    connectionData).apply {
    emptyText = (BdIdeRegistryUtil.RFS_DEFAULT_TIMEOUT / 1000).toString()
    getTextComponent().toolTipText = MessagesBundle.message("s3.operations.timeout.hint")
  }

  init {
    operationTimeout.withNumberValidatorOrEmpty(uiDisposable)
  }

  //region Basic auth settings
  override val enableBasicAuthCheckbox =
    CheckBoxField(SparkConnectionData::enableBasicAuth, CommonSettingsKeys.ENABLE_BASIC_AUTH_KEY, connectionData)

  private val basicAuthCredentials =
    CredentialsHolder(connectionData, SparkConnectionData.BASIC_CREDENTIALS_ID, uiDisposable, coroutineScope)

  override val basicAuthLoginField =
    UsernameNamedField(CommonSettingsKeys.BASIC_AUTH_LOGIN_KEY, basicAuthCredentials)
      .withNotEmptyValidator(uiDisposable, MessagesBundle.message("settings.basicAuth.field"))

  override val basicAuthPasswordField =
    PasswordNamedField(CommonSettingsKeys.BASIC_AUTH_PASSWORD_KEY, basicAuthCredentials)
  //endregion Basic auth settings

  //region Proxy settings
  override var proxyEnableComboBox =
    ComboBoxField(SparkConnectionData::proxyEnableType,
                  CommonSettingsKeys.PROXY_ENABLE_TYPE_KEY,
                  connectionData,
                  ProxyEnableType.entries.toTypedArray()) { it.title }

  override val proxyTypeComboBox =
    ComboBoxField(SparkConnectionData::proxyType,
                  CommonSettingsKeys.PROXY_TYPE_KEY,
                  connectionData,
                  ProxyType.entries.toTypedArray())

  override val proxyAuthEnabledCheckbox =
    CheckBoxField(SparkConnectionData::proxyAuthEnabled, CommonSettingsKeys.ENABLE_PROXY_AUTH_KEY, connectionData)

  override val proxyHostField =
    StringNamedField(SparkConnectionData::proxyHost, CommonSettingsKeys.PROXY_HOST_KEY, connectionData)
      .withProxyHostValidator(uiDisposable, proxyEnableComboBox)

  override val proxyPortField =
    IntNamedField(SparkConnectionData::proxyPort, CommonSettingsKeys.PROXY_PORT_KEY, connectionData)
      .withPortValidator(uiDisposable)

  private val proxyCredentials =
    CredentialsHolder(connectionData, SparkConnectionData.PROXY_CREDENTIALS_ID, uiDisposable, coroutineScope)

  override val proxyLoginField =
    UsernameNamedField(CommonSettingsKeys.PROXY_LOGIN_KEY, proxyCredentials)

  override val proxyPasswordField =
    PasswordNamedField(CommonSettingsKeys.PROXY_PASSWORD_KEY, proxyCredentials)
  //endregion Proxy settings

  init {
    //Refresh url by URI which can be changed by SSH config
    url.getTextComponent().text = connectionData.uri
    SparkMonitoringSettingsCollector.Util.getInstance().initPanel(this)
  }
}