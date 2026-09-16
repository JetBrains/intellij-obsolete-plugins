package com.jetbrains.hadoop.monitoring.settings

import com.intellij.bigdatatools.coreUi.util.BdIdeRegistryUtil
import com.intellij.bigdatatools.coreUi.settings.CommonSettingsKeys
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.settings.ConnectionSettingsListener
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.coreUi.settings.ModificationKey
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
import com.intellij.bigdatatools.coreUi.fields.WrappedComponent
import com.intellij.bigdatatools.coreUi.settings.withNotEmptyValidator
import com.intellij.bigdatatools.coreUi.settings.withNumberValidatorOrEmpty
import com.intellij.bigdatatools.coreUi.settings.withPortValidator
import com.intellij.bigdatatools.coreUi.settings.withProxyHostValidator
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.ActionLink
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.jetbrains.bigdatatools.common.monitoring.HttpSettingsCustomizer
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.hadoop.monitoring.statistics.HadoopSettingsCollector
import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle
import kotlinx.coroutines.CoroutineScope

class HadoopSettingsCustomizer(
  project: Project,
  connectionData: HadoopConnectionData,
  uiDisposable: Disposable,
  coroutineScope: CoroutineScope
) : HttpSettingsCustomizer<HadoopConnectionData>(uiDisposable, project, connectionData) {
  override val operationTimeout: StringNonRequiredField<HadoopConnectionData> = StringNonRequiredField(
    HadoopConnectionData::operationTimeout,
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
    CheckBoxField(HadoopConnectionData::enableBasicAuth, CommonSettingsKeys.ENABLE_BASIC_AUTH_KEY, connectionData)

  private val basicAuthCredentials =
    CredentialsHolder(connectionData, HadoopConnectionData.BASIC_CREDENTIALS_ID, uiDisposable, coroutineScope)

  override val basicAuthLoginField =
    UsernameNamedField(CommonSettingsKeys.BASIC_AUTH_LOGIN_KEY, basicAuthCredentials)
      .withNotEmptyValidator(uiDisposable, MessagesBundle.message("settings.basicAuth.field"))

  override val basicAuthPasswordField =
    PasswordNamedField(CommonSettingsKeys.BASIC_AUTH_PASSWORD_KEY, basicAuthCredentials)
  //endregion Basic auth settings

  //region Proxy settings
  override var proxyEnableComboBox = ComboBoxField(HadoopConnectionData::proxyEnableType,
                                                   CommonSettingsKeys.PROXY_ENABLE_TYPE_KEY,
                                                   connectionData,
                                                   ProxyEnableType.entries.toTypedArray()) { it.title }

  override val proxyTypeComboBox = ComboBoxField(HadoopConnectionData::proxyType,
                                                 HadoopSettingsKeys.PROXY_TYPE_KEY,
                                                 connectionData,
                                                 ProxyType.entries.toTypedArray())

  override val proxyAuthEnabledCheckbox = CheckBoxField(HadoopConnectionData::proxyAuthEnabled,
                                                        CommonSettingsKeys.ENABLE_PROXY_AUTH_KEY,
                                                        connectionData)

  override val proxyHostField = StringNamedField(HadoopConnectionData::proxyHost,
                                                 CommonSettingsKeys.PROXY_HOST_KEY,
                                                 connectionData).withProxyHostValidator(uiDisposable, proxyEnableComboBox)

  override val proxyPortField = IntNamedField(HadoopConnectionData::proxyPort,
                                              CommonSettingsKeys.PROXY_PORT_KEY,
                                              connectionData).withPortValidator(uiDisposable)

  private val proxyCredentials =
    CredentialsHolder(connectionData, HadoopConnectionData.PROXY_CREDENTIALS_ID, uiDisposable, coroutineScope)

  override val proxyLoginField =
    UsernameNamedField(CommonSettingsKeys.PROXY_LOGIN_KEY, proxyCredentials)

  override val proxyPasswordField =
    PasswordNamedField(CommonSettingsKeys.PROXY_PASSWORD_KEY, proxyCredentials)
  //endregion Proxy settings

  val sparkMonitoringComboBox = ComboBoxField(HadoopConnectionData::sparkMonitoringDriverId,
                                              HadoopSettingsKeys.SPARK_MONITORING_TYPE_KEY,
                                              connectionData,
                                              getSparkMonitoringConnectionIds(),
                                              ::getSparkMonitoringConnectionNames)

  private fun getSparkMonitoringConnectionIds(): Array<String> {
    val connections = RfsConnectionDataManager.instance?.getConnectionsByGroupId(BdtConnectionType.SPARK_MONITORING.id, project)
                      ?: emptyList()
    return if (connections.isEmpty()) arrayOf("") else arrayOf("") + connections.map { it.innerId }.toTypedArray()
  }

  private fun getSparkMonitoringConnectionNames(innerId: String): String? {
    return RfsConnectionDataManager.instance?.getConnectionById(project, innerId)?.name
  }

  override fun getDefaultFields() = super.getDefaultFields() + sparkMonitoringComboBox

  private val sparkMonitoringSettingsListener = object : ConnectionSettingsListener {

    override fun onConnectionAdded(project: Project?, newConnectionData: ConnectionData) = initSparkMonitoringComboBox(newConnectionData)

    override fun onConnectionRemoved(project: Project?, removedConnectionData: ConnectionData) = initSparkMonitoringComboBox(
      removedConnectionData)

    override fun onConnectionModified(project: Project?,
                                      connectionData: ConnectionData,
                                      modified: Collection<ModificationKey>) {
      initSparkMonitoringComboBox(connectionData)
    }
  }

  private fun initSparkMonitoringComboBox(connectionData: ConnectionData) {
    if (connectionData.groupId != BdtConnectionType.SPARK_MONITORING.id) return
    sparkMonitoringComboBox.setItems(getSparkMonitoringConnectionIds(), ::getSparkMonitoringConnectionNames)
  }

  init {
    RfsConnectionDataManager.instance?.addListener(sparkMonitoringSettingsListener)

    Disposer.register(uiDisposable,
                      Disposable { RfsConnectionDataManager.instance?.removeListener(sparkMonitoringSettingsListener) })

    //Refresh url by URI which can be changed by SSH config
    url.getTextComponent().text = connectionData.uri
    HadoopSettingsCollector.Util.getInstance().initPanel(this)
  }

  override fun getDefaultComponent(fields: List<WrappedComponent<in HadoopConnectionData>>, conn: HadoopConnectionData) = panel {
    setHttpSettingsBlock()

    row(HadoopMessagesBundle.message("settings.integration.spark.monitoring")) {
      cell(sparkMonitoringComboBox.getComponent()).align(AlignX.FILL).resizableColumn()

      cell(ActionLink(HadoopMessagesBundle.message("settings.integration.spark.monitoring.add")) {
        MonitoringServiceProvider.createNewMonitoringConnection(BdtConnectionType.SPARK_MONITORING.id, project, conn.isPerProject)
      })
    }.topGap(TopGap.SMALL)
  }
}