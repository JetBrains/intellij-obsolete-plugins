package com.intellij.bigdatatools.zeppelin.settings

import com.intellij.bigdatatools.coreUi.connection.ProxyEnableType
import com.intellij.bigdatatools.coreUi.connection.ProxyType
import com.intellij.bigdatatools.coreUi.fields.CheckBoxField
import com.intellij.bigdatatools.coreUi.fields.ComboBoxField
import com.intellij.bigdatatools.coreUi.fields.CookieComponent
import com.intellij.bigdatatools.coreUi.fields.CredentialsHolder
import com.intellij.bigdatatools.coreUi.fields.CustomObjectEditComponent
import com.intellij.bigdatatools.coreUi.fields.IntNamedField
import com.intellij.bigdatatools.coreUi.fields.NullableCheckBoxField
import com.intellij.bigdatatools.coreUi.fields.PasswordNamedField
import com.intellij.bigdatatools.coreUi.fields.StringNamedField
import com.intellij.bigdatatools.coreUi.fields.UsernameNamedField
import com.intellij.bigdatatools.coreUi.fields.WrappedComponent
import com.intellij.bigdatatools.coreUi.settings.CommonSettingsKeys
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.settings.defaultui.AuthPanel.setAuthBlock
import com.intellij.bigdatatools.coreUi.settings.defaultui.HostAndPortChangeListener
import com.intellij.bigdatatools.coreUi.settings.defaultui.HostAndPortProvider
import com.intellij.bigdatatools.coreUi.settings.defaultui.HttpProxyPanel.setProxyBlock
import com.intellij.bigdatatools.coreUi.settings.defaultui.SettingsPanelCustomizer.Companion.setupLoginPasswordAnonymous
import com.intellij.bigdatatools.coreUi.settings.defaultui.registerOnTextComponent
import com.intellij.bigdatatools.coreUi.settings.withNotEmptyValidator
import com.intellij.bigdatatools.coreUi.settings.withPortValidator
import com.intellij.bigdatatools.coreUi.settings.withProxyHostValidator
import com.intellij.bigdatatools.coreUi.settings.withUrlValidator
import com.intellij.bigdatatools.coreUi.settings.withValidator
import com.intellij.bigdatatools.coreUi.ui.block
import com.intellij.bigdatatools.coreUi.ui.row
import com.intellij.bigdatatools.coreUi.util.BdIdeRegistryUtil
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.bigdatatools.zeppelin.ztools.settings.ZtoolsDialogBuilder
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.bigdatatools.common.connection.oauth.OAuth2
import com.jetbrains.bigdatatools.common.connection.tunnel.ui.SshTunnelComponent
import com.jetbrains.bigdatatools.common.settings.defaultui.SettingsPanelCustomizerExImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.apache.http.impl.cookie.BasicClientCookie
import java.awt.event.ItemEvent
import java.util.function.Supplier
import javax.swing.JLabel
import javax.swing.JTextField

class ZeppelinSettingsCustomizer(val project: Project,
                                 val connectionData: ZeppelinConnectionData,
                                 uiDisposable: Disposable,
                                 coroutineScope: CoroutineScope) : SettingsPanelCustomizerExImpl<ZeppelinConnectionData>(), HostAndPortProvider {
  internal val nameField = StringNamedField(ConnectionData::name, CommonSettingsKeys.NAME_KEY, connectionData).withNotEmptyValidator(
    uiDisposable, MessagesBundle.message("validator.nameField"))

  internal val url = StringNamedField(ConnectionData::uri, CommonSettingsKeys.URL_KEY, connectionData)
    .withUrlValidator(uiDisposable)

  private val cookiesComponent =
    CookieComponent<ZeppelinConnectionData>(CommonSettingsKeys.COOKIE_KEY, project).also {
      coroutineScope.launch {
        val cookies = connectionData.getCookie()
        if (cookies != null) {
          it.initCookies(cookies)
        }
      }
    }

  private val mainCredentials = CredentialsHolder(connectionData, null, uiDisposable, coroutineScope)
  internal val login = UsernameNamedField(CommonSettingsKeys.LOGIN_KEY, mainCredentials)
  internal val password = PasswordNamedField(CommonSettingsKeys.PASS_KEY, mainCredentials)

  internal val anonymous = CheckBoxField(ConnectionData::anonymous, CommonSettingsKeys.ANONYMOUS_KEY, connectionData)

  internal val systemNotificationEnabled = CheckBoxField(ZeppelinConnectionData::systemNotificationEnabled,
                                                         ZeppelinSettingsKeys.NOTIFICATION_ENABLE_KEY, connectionData)

  internal val notificationAfter = IntNamedField(ZeppelinConnectionData::notifyAfter, ZeppelinSettingsKeys.NOTIFICATION_AFTER_KEY,
                                                 connectionData).withValidator(uiDisposable) {
    if (it.isBlank()) return@withValidator ZepMessagesBundle.message("notify.timeout.validation.nonempty")
    val time = it.toIntOrNull() ?: return@withValidator ZepMessagesBundle.message("notify.timeout.validation.integer")
    if (time < 0) return@withValidator ZepMessagesBundle.message("notify.timeout.validation.positive")
    return@withValidator null
  }.also {
    val textComponent = it.getTextComponent() as? JTextField
    textComponent?.columns = 4
  }

  internal val scalaVersion = ComboBoxField(ZeppelinConnectionData::scalaVersion, ZeppelinSettingsKeys.SCALA_VERSION_KEY, connectionData,
                                            LibraryVersion.getSupportedScalaVersion()).apply {
    getComponent().apply {
      prototypeDisplayValue = "999"
      addItemListener { e ->
        val event = e ?: return@addItemListener
        if (event.item.toString() == "2.10" && event.stateChange == ItemEvent.SELECTED) {
          setZToolsVisible(false)
        }
        else if (event.stateChange == ItemEvent.SELECTED) {
          setZToolsVisible(true)
        }
        getValidationErrors()
      }
    }
  }

  internal val sparkVersion = StringNamedField(ZeppelinConnectionData::sparkVersion, ZeppelinSettingsKeys.SPARK_VERSION_KEY,
                                               connectionData, columns = 5).apply {
    withValidator(uiDisposable, Supplier {
      val sparkVersionString = this.getTextComponent().text
      when {
        sparkVersionString.isBlank() -> return@Supplier ValidationInfo(ZepMessagesBundle.message("spark.validation.is.empty"),
                                                                       this.getComponent())
        !Regex("[0-9]{1,2}[.][0-9]{1,2}[.][0-9]{1,2}").matches(sparkVersionString) -> return@Supplier ValidationInfo(
          ZepMessagesBundle.message("spark.validation.format"), this.getComponent()).asWarning()
      }
      val sparkVersion = LibraryVersion(sparkVersionString)

      val scala = scalaVersion.getValue()
      when {
        scala == "2.10" && sparkVersion > LibraryVersion("2.2.3") ->
          ValidationInfo(ZepMessagesBundle.message("spark.validation.spark.scala.version", scala, "0.9.1", "2.2.3"),
                         this.getComponent())
        scala == "2.11" && (sparkVersion < LibraryVersion("1.2.0") || sparkVersion > LibraryVersion("2.4.8")) ->
          ValidationInfo(ZepMessagesBundle.message("spark.validation.spark.scala.version", scala, "1.2.0", "2.4.8"),
                         this.getComponent())
        scala == "2.12" && sparkVersion < LibraryVersion("2.4.0") ->
          ValidationInfo(ZepMessagesBundle.message("spark.validation.spark.scala.version", scala, "2.4.0", "3.x.x"),
                         this.getComponent())
        else -> null
      }
    })
  }

  internal val hadoopVersion = StringNamedField(ZeppelinConnectionData::hadoopVersion, ZeppelinSettingsKeys.HADOOP_VERSION_KEY,
                                                connectionData, columns = 5).apply {
    withValidator(uiDisposable, Supplier {
      when {
        this.getTextComponent().text.isBlank() -> ValidationInfo(ZepMessagesBundle.message("hadoop.validation.is.empty"),
                                                                 this.getComponent())
        !Regex("[0-9]{1,2}[.][0-9]{1,2}[.][0-9]{1,2}").matches(this.getTextComponent().text) -> ValidationInfo(
          ZepMessagesBundle.message("hadoop.validation.format"), this.getComponent()).asWarning()
        else -> null
      }
    })
  }

  internal val zeppelinVersion = StringNamedField(ZeppelinConnectionData::zeppelinVersion, ZeppelinSettingsKeys.ZEPPELIN_VERSION_KEY,
                                                  connectionData, columns = 5).apply {
    emptyText = ZepMessagesBundle.message("settings.zeppelin.version.empty.text")
  }


  internal val flinkVersion = StringNamedField(ZeppelinConnectionData::flinkVersion,
                                               ZeppelinSettingsKeys.FLINK_VERSION_KEY,
                                               connectionData, columns = 5).apply {
    withValidator(uiDisposable, Supplier {
      val flinkVersionString = this.getTextComponent().text
      when {
        flinkVersionString.isBlank() -> return@Supplier ValidationInfo(ZepMessagesBundle.message("flink.validation.is.empty"),
                                                                       this.getComponent())
        !Regex("[0-9][.][0-9]{1,2}[.][0-9]").matches(flinkVersionString) -> return@Supplier ValidationInfo(
          ZepMessagesBundle.message("flink.validation.format"), this.getComponent()).asWarning()
      }
      val flinkVersion = LibraryVersion(flinkVersionString)

      val scala = scalaVersion.getValue()
      when {
        scala == "2.10" ->
          ValidationInfo(ZepMessagesBundle.message("flink.validation.supported.scala.version"),
                         this.getComponent())
        scala == "2.11" && (flinkVersion < LibraryVersion("1.8.0") || flinkVersion > LibraryVersion("1.14.5")) ->
          ValidationInfo(ZepMessagesBundle.message("flink.validation.scala.version", scala, "1.8.0", "1.14.5"),
                         this.getComponent())
        scala == "2.12" && flinkVersion < LibraryVersion("1.8.0") ->
          ValidationInfo(ZepMessagesBundle.message("flink.validation.scala.version", scala, "1.8.0", "1.1x.x"),
                         this.getComponent())
        else -> null
      }
    })
  }

  internal val enableBasicAuthCheckbox =
    CheckBoxField(ZeppelinConnectionData::enableBasicAuth, CommonSettingsKeys.ENABLE_BASIC_AUTH_KEY, connectionData)

  private val basicAuthCredentials =
    CredentialsHolder(connectionData, ZeppelinConnectionData.NGINX_CREDENTIALS_ID, uiDisposable, coroutineScope)

  internal val basicAuthLoginField =
    UsernameNamedField(CommonSettingsKeys.BASIC_AUTH_LOGIN_KEY, basicAuthCredentials)
      .withNotEmptyValidator(uiDisposable, MessagesBundle.message("settings.basicAuth.field"))

  internal val basicAuthPasswordField =
    PasswordNamedField(CommonSettingsKeys.BASIC_AUTH_PASSWORD_KEY, basicAuthCredentials)

  internal val proxyEnableComboBox =
    ComboBoxField(ZeppelinConnectionData::proxyEnableType, CommonSettingsKeys.PROXY_ENABLE_TYPE_KEY,
                  connectionData, ProxyEnableType.entries.toTypedArray()) { it.title }

  internal val proxyTypeComboBox =
    ComboBoxField(ZeppelinConnectionData::proxyType, CommonSettingsKeys.PROXY_TYPE_KEY, connectionData, ProxyType.entries.toTypedArray())

  internal val proxyHostField =
    StringNamedField(ZeppelinConnectionData::proxyHost, CommonSettingsKeys.PROXY_HOST_KEY, connectionData)
      .withProxyHostValidator(uiDisposable, proxyEnableComboBox)

  internal val proxyPortField =
    IntNamedField(ZeppelinConnectionData::proxyPort, CommonSettingsKeys.PROXY_PORT_KEY, connectionData)
      .withPortValidator(uiDisposable)

  internal val proxyAuthEnabledCheckbox =
    CheckBoxField(ZeppelinConnectionData::proxyAuthEnabled, CommonSettingsKeys.ENABLE_PROXY_AUTH_KEY, connectionData)

  private val proxyCredentials =
    CredentialsHolder(connectionData, ZeppelinConnectionData.PROXY_CREDENTIALS_ID, uiDisposable, coroutineScope)

  internal val proxyLoginField =
    UsernameNamedField(CommonSettingsKeys.PROXY_LOGIN_KEY, proxyCredentials)

  internal val proxyPasswordField =
    PasswordNamedField(CommonSettingsKeys.PROXY_PASSWORD_KEY, proxyCredentials)

  internal val enableZtools =
    NullableCheckBoxField(ZeppelinConnectionData::isZtoolsEnabled,
                          ZeppelinSettingsKeys.ENABLE_ZTOOLS,
                          connectionData).also { checkBox ->
      checkBox.checkBoxField.addItemListener { event ->
        setZtoolsOptionsVisible(event.stateChange == ItemEvent.SELECTED)
      }
    }

  private val ztoolsSettings =
    CustomObjectEditComponent(ZeppelinConnectionData::ztoolsConf,
                              ZeppelinSettingsKeys.ZTOOLS_CONFIG,
                              ZepMessagesBundle.message("ztools.settings.show"),
                              connectionData) {
      ZtoolsDialogBuilder.showAndGet(project, it)
    }

  private val ztoolsIsDisabledLabel = JBLabel(ZepMessagesBundle.message("ztools.disabled.2.10.label"))
  internal val tunnelComponent = SshTunnelComponent(project, uiDisposable, connectionData, this)
  internal val enableTunnelField = tunnelComponent.isEnabledCheckBox

  private fun Panel.slackPanel() {
    row(ztoolsIsDisabledLabel)
    row {
      cell(enableZtools.getComponent())
        .contextHelp(ZepMessagesBundle.message("ztools.tooltip"))
    }
    setZToolsVisible(scalaVersion.getComponent().selectedItem?.toString() != "2.10")
  }

  private fun Panel.toolsSettingsPanel() {
    row(ztoolsSettings.getComponent())
    setZtoolsOptionsVisible(enableZtools.getComponent().isSelected)
  }

  init {
    //Refresh url by URI which can be changed by SSH config
    url.getTextComponent().text = connectionData.uri
  }

  override fun getDefaultFields() = listOf<WrappedComponent<in ZeppelinConnectionData>>(nameField, url, login, password, anonymous,
                                                                                        scalaVersion, sparkVersion, hadoopVersion,
                                                                                        flinkVersion, zeppelinVersion)

  override fun getAdditionalFields() = listOf<WrappedComponent<in ZeppelinConnectionData>>(cookiesComponent, enableBasicAuthCheckbox,
                                                                                           enableZtools,
                                                                                           ztoolsSettings,
                                                                                           basicAuthLoginField, basicAuthPasswordField,

                                                                                           proxyEnableComboBox, proxyTypeComboBox,
                                                                                           proxyPortField, proxyHostField,
                                                                                           proxyAuthEnabledCheckbox, proxyLoginField,
                                                                                           proxyPasswordField, tunnelComponent,
                                                                                           notificationAfter, systemNotificationEnabled)

  override fun getDefaultComponent(fields: List<WrappedComponent<in ZeppelinConnectionData>>,
                                   conn: ZeppelinConnectionData) = panel {

    setupLoginPasswordAnonymous(login.getComponent(), password.getComponent(), anonymous)

    row(nameField)
    row(url)
    row(login.labelComponent) {
      cell(login.getComponent()).align(AlignX.FILL).resizableColumn()
      cell(anonymous.getComponent())
    }
    row(password)
    if (BdIdeRegistryUtil.isInternalBrowserOauthEnabled())
      createBrowserAuthPanel()
  }

  override fun getAdditionalComponent(conn: ZeppelinConnectionData) = panel {
    collapsibleGroup(ZepMessagesBundle.message("settings.versions.header")) {
      createVersionsPanel()
    }.apply {
      expanded = isLibrariesVersionsShown()
    }.addExpandedListener {
      setLibrariesVersionsShown(it)
    }

    collapsibleGroup(ZepMessagesBundle.message("settings.connection.header")) {
      row(zeppelinVersion)
      setAuthBlock(enableBasicAuthCheckbox, basicAuthLoginField, basicAuthPasswordField)
      setProxyBlock(proxyEnableComboBox, proxyTypeComboBox, proxyHostField, proxyPortField,
                    proxyAuthEnabledCheckbox, proxyLoginField, proxyPasswordField)
      block(tunnelComponent.getComponent())
    }.apply {
      expanded = isAdvancedSettingsShown()
    }.addExpandedListener {
      setAdvancedSettingsShown(it)
    }

    collapsibleGroup(ZepMessagesBundle.message("settings.notification.header")) {
      createNotificationPanel()
    }.apply {
      expanded = isNotificationsShown()
    }.addExpandedListener {
      setNotificationShown(it)
    }

    slackPanel()
    toolsSettingsPanel()
  }

  private fun Panel.createNotificationPanel() {
    row(systemNotificationEnabled.getComponent())
    row(notificationAfter.labelComponent) {
      cell(notificationAfter.getComponent())
      cell(JLabel(ZepMessagesBundle.message("settings.notification.sec")))
    }
  }

  private fun setZToolsVisible(visible: Boolean) {
    ztoolsIsDisabledLabel.isVisible = !visible
    ztoolsSettings.isVisible = !visible
  }

  private fun setZtoolsOptionsVisible(visible: Boolean) {
    ztoolsSettings.getComponent().isVisible = visible
  }

  private fun Panel.createVersionsPanel() {
    row {
      cell(JBLabel(ZeppelinSettingsKeys.SCALA_VERSION_KEY.label)).gap(RightGap.SMALL)
      cell(scalaVersion.getComponent())

      listOf(sparkVersion, hadoopVersion, flinkVersion).forEach {
        cell(it.labelComponent).gap(RightGap.SMALL)
        cell(it.getComponent())
      }
    }
  }

  private fun Panel.createBrowserAuthPanel() {
    val actionLink = ActionLink(MessagesBundle.message("oauth.settings.open.button")) {
      val cookies = OAuth2.showAndWaitCookies(url.getTextComponent().text) ?: return@ActionLink
      cookiesComponent.cookies = cookies.map {
        BasicClientCookie(it.name, it.value).apply {
          this.expiryDate = it.expires
          this.isSecure = it.secure
          this.path = it.path
          this.domain = it.domain
        }
      }
    }

    row {
      cell(actionLink)
        .contextHelp(ZepMessagesBundle.message("oauth.tooltip"))
    }
  }

  override fun registerChangeListener(listener: HostAndPortChangeListener) {
    registerOnTextComponent(url.getTextComponent(), listener)
  }

  companion object {
    private const val ADVANCED_SETTINGS_SHOW_ID = "com.jetbrains.bigdatatools.zeppelin.connections.advanced.show"
    private const val LIBRARIES_VERSIONS_SHOW_ID = "com.jetbrains.bigdatatools.zeppelin.connections.libraries.show"
    private const val NOTIFICATION_SHOW_ID = "com.jetbrains.bigdatatools.zeppelin.connections.notification.show"

    private fun isAdvancedSettingsShown(): Boolean = PropertiesComponent.getInstance().getBoolean(ADVANCED_SETTINGS_SHOW_ID, false)

    private fun setAdvancedSettingsShown(value: Boolean) {
      if (!value && !isAdvancedSettingsShown()) {
        return
      }
      PropertiesComponent.getInstance().setValue(ADVANCED_SETTINGS_SHOW_ID, value)
    }

    private fun isLibrariesVersionsShown(): Boolean = PropertiesComponent.getInstance().getBoolean(LIBRARIES_VERSIONS_SHOW_ID, false)
    private fun isNotificationsShown(): Boolean = PropertiesComponent.getInstance().getBoolean(NOTIFICATION_SHOW_ID, false)

    private fun setLibrariesVersionsShown(value: Boolean) {
      if (!value && !isLibrariesVersionsShown()) {
        return
      }
      PropertiesComponent.getInstance().setValue(LIBRARIES_VERSIONS_SHOW_ID, value)
    }

    private fun setNotificationShown(value: Boolean) {
      if (!value && !isNotificationsShown()) {
        return
      }
      PropertiesComponent.getInstance().setValue(NOTIFICATION_SHOW_ID, value)
    }
  }
}