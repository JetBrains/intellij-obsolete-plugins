package com.intellij.bigdatatools.emr.settings

import com.intellij.bigdatatools.awsBase.connection.auth.AuthenticationType
import com.intellij.bigdatatools.awsBase.ui.AwsComponentsBuilder
import com.intellij.bigdatatools.awsBase.ui.AwsCredentialsComponents
import com.intellij.bigdatatools.awsBase.utils.AwsMessagesBundle
import com.intellij.bigdatatools.aws.driver.AwsCompatibleSettingsCustomizer
import com.intellij.bigdatatools.coreUi.settings.defaultui.HostAndPortChangeListener
import com.intellij.bigdatatools.coreUi.settings.defaultui.HostAndPortProvider
import com.intellij.bigdatatools.coreUi.fields.ComboBoxField
import com.intellij.bigdatatools.coreUi.fields.WrappedComponent
import com.intellij.bigdatatools.coreUi.ui.shortRow
import com.intellij.bigdatatools.emr.statistic.EmrSettingsCollector
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.intellij.bigdatatools.coreUi.ui.row
import kotlinx.coroutines.CoroutineScope

class EmrSettingsCustomizer(
  project: Project,
  connectionData: EmrConnectionData,
  uiDisposable: Disposable,
  coroutineScope: CoroutineScope
) : AwsCompatibleSettingsCustomizer<EmrConnectionData>(project, connectionData, uiDisposable, coroutineScope), HostAndPortProvider {

  private val authComponents = AwsCredentialsComponents(connectionData, uiDisposable,
                                                        arrayOf(AuthenticationType.DEFAULT, AuthenticationType.KEY_PAIR,
                                                                AuthenticationType.PROFILE_FROM_CREDENTIALS_FILE))

  @Suppress("UNCHECKED_CAST")
  internal val authTypeChooser = authComponents.authTypeChooser as ComboBoxField<EmrConnectionData, String>

  internal val region = AwsComponentsBuilder.createRegionComponent(connectionData, null)

  internal val profileName = authComponents.profileName
  internal val userCustomConfigPath = authComponents.userCustomConfigPath
  internal val profileConfigPath = authComponents.profileConfigPath
  internal val profileCredentialsPath = authComponents.profileCredentialsPath


  init {
    authTypeChooser.getComponent().addItemListener {
      updateAuthStatus()
    }
    updateAuthStatus()
    EmrSettingsCollector.Util.getInstance().initPanel(this)
  }

  override fun getDefaultComponent(fields: List<WrappedComponent<in EmrConnectionData>>, conn: EmrConnectionData) = panel {
    row(nameField)
    shortRow(region)

    shortRow(AwsMessagesBundle.message("s3.auth.type"), authTypeChooser.getComponent())

    indent {
      panel {
        row(accessKey)
        row(secretKey)
        shortRow(profileName).layout(RowLayout.INDEPENDENT)
        row(userCustomConfigPath.getComponent())
        row(profileCredentialsPath)
        row(profileConfigPath)
        authComponents.connStatusPanel(this)
        row(authComponents.actionLink)
      }
    }

    proxyPanel.create(this)
  }

  override fun getDefaultFields(): List<WrappedComponent<in EmrConnectionData>> {
    val fields: List<WrappedComponent<in EmrConnectionData>> = listOf(region, authTypeChooser, profileName,
                                                                      userCustomConfigPath, profileCredentialsPath, profileConfigPath)

    return super.getDefaultFields() + fields
  }

  private fun updateAuthStatus() {
    val authType = authTypeChooser.getValue()

    accessKey.isVisible = authType == AuthenticationType.KEY_PAIR.id
    secretKey.isVisible = accessKey.isVisible

    authComponents.updateVisibility()
  }

  override fun onTestConnectionFinish(createdDriver: Driver?, success: Boolean) {
    authComponents.updateConnectionStatus()
  }

  override fun registerChangeListener(listener: HostAndPortChangeListener) = Unit
}