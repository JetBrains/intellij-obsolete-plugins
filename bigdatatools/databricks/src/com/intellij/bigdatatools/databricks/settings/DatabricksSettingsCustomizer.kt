package com.intellij.bigdatatools.databricks.settings

import com.intellij.bigdatatools.coreUi.fields.NullableComboBoxField
import com.intellij.bigdatatools.coreUi.fields.WrappedComponent
import com.intellij.bigdatatools.coreUi.fields.WrappedNamedField
import com.intellij.bigdatatools.coreUi.settings.ModificationKey
import com.intellij.bigdatatools.coreUi.settings.withValidator
import com.intellij.bigdatatools.coreUi.ui.row
import com.intellij.bigdatatools.coreUi.ui.shortRow
import com.intellij.bigdatatools.databricks.auth.AzureCliWrapper
import com.intellij.bigdatatools.databricks.cli.DatabricksCliManager
import com.intellij.bigdatatools.databricks.client.DatabricksConnType
import com.intellij.bigdatatools.databricks.rfs.DatabricksConnectionData
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.bigdatatools.databricks.util.DatabricksProfiles
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.util.and
import com.intellij.openapi.observable.util.equalsTo
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.observable.util.notEqualsTo
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.AsyncProcessIcon
import com.jetbrains.bigdatatools.common.monitoring.TunnelableSettingsCustomizer
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.settings.fields.RadioGroupField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.event.ItemEvent
import java.io.File
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent

class DatabricksSettingsCustomizer(
  project: Project,
  connectionData: DatabricksConnectionData,
  private val uiDisposable: Disposable,
  private val coroutineScope: CoroutineScope,
) : TunnelableSettingsCustomizer<DatabricksConnectionData>(connectionData, project, uiDisposable) {

  init { //Hack to save configs if it was changed
    FileDocumentManager.getInstance().saveAllDocuments()
  }

  private val authTypeProperty = AtomicProperty(connectionData.connType)

  private val azureCLIInstalled = AtomicProperty(false)
  private val azureLoggedIn = AtomicProperty(false)
  private val azureVersion = AtomicProperty("")
  private val azureUser = AtomicProperty("")

  private val databricksCliInstalled = AtomicProperty(false)

  private val isGettingAzureCLIVersion = AtomicProperty(false)
  private val isGettingDatabricksCLIVersion = AtomicProperty(false)

  private val authType = RadioGroupField(DatabricksConnectionData::connType, ModificationKey(DatabricksBundle.message("settings.property.auth")), connectionData, DatabricksConnType.entries).also {
    it.addItemListener {
      authTypeProperty.set(it.getValue())
      authTypeChanged(it.getValue())
    }
  }

  private val profilesField = NullableComboBoxField(DatabricksConnectionData::profile, ModificationKey(DatabricksBundle.message("settings.property.profile")), connectionData, DatabricksProfiles.getPropertiesOrEmpty(), "") { it }.withValidator(uiDisposable) {
    if (it.isNullOrEmpty()) DatabricksBundle.message("profile.validation.empty") else null
  }.apply {
    getComponent().addItemListener { listener ->
      if (listener.stateChange == ItemEvent.SELECTED) {
        val host = DatabricksProfiles.getProfileHost(getComponent().item) ?: return@addItemListener
        (url as WrappedNamedField).setValue(host)
      }
    }
  }

  override fun getDefaultFields(): List<WrappedComponent<in DatabricksConnectionData>> = listOf(nameField, url, authType, profilesField)

  override fun onTestConnectionFinish(createdDriver: Driver?, success: Boolean) {
    updateDatabricksCLIBlock()
  }

  override fun getDefaultComponent(
    fields: List<WrappedComponent<in DatabricksConnectionData>>,
    conn: DatabricksConnectionData,
  ) = panel {
    row(nameField)
    row(url).visibleIf(authTypeProperty.notEqualsTo(DatabricksConnType.PROFILE))
    shortRow(authType)

    // Profile selection for PROFILE auth type.
    indent {
      row {
        label(DatabricksBundle.message("settings.property.profile"))
        cell(profilesField.getComponent()).align(AlignX.FILL).resizableColumn().gap(RightGap.SMALL)
        actionButton(DumbAwareAction.create(DatabricksBundle.message("open.profiles.config"), AllIcons.Actions.Edit, ::openDatabricksCfg)).gap(RightGap.SMALL)
        actionButton(DumbAwareAction.create(DatabricksBundle.message("reload.profiles.config"), AllIcons.Actions.Refresh) {
          profilesField.setItems(DatabricksProfiles.getPropertiesOrEmpty())
        })
      }
    }.visibleIf(authTypeProperty.equalsTo(DatabricksConnType.PROFILE))

    // AZURE-type auth.
    indent {
      row {
        label(DatabricksBundle.message("settings.cli.version")).widthGroup("A")
        text("").align(AlignX.FILL).resizableColumn().bindText(azureVersion)
      }.visibleIf(azureCLIInstalled)

      row {
        label(DatabricksBundle.message("settings.azure.cli.missing"))
        browserLink(DatabricksBundle.message("settings.cli.install"), "https://learn.microsoft.com/en-us/cli/azure/install-azure-cli")
      }.visibleIf(azureCLIInstalled.not().and(isGettingAzureCLIVersion.not()))

      row {
        label(DatabricksBundle.message("settings.cli.user")).widthGroup("A")
        text("").bindText(azureUser)
      }.visibleIf(azureCLIInstalled.and(azureLoggedIn))

      row {
        label(DatabricksBundle.message("account.is.not.selected"))
      }.visibleIf(azureCLIInstalled.and(azureLoggedIn.not()))

      row {
        cell(AsyncProcessIcon("Loading"))
        label(DatabricksBundle.message("settings.cli.version.retrieving"))
      }.visibleIf(azureCLIInstalled.not().and(isGettingAzureCLIVersion))
    }.visibleIf(authTypeProperty.equalsTo(DatabricksConnType.AZURE))

    // DATABRICKS-type auth.
    indent {
      row {
        label(DatabricksBundle.message("cli.is.installed")).apply {
          component.icon = AllIcons.General.InspectionsOK
        }
      }.visibleIf(databricksCliInstalled)

      row {
        label(DatabricksBundle.message("cli.is.not.installed")).apply {
          component.icon = AllIcons.General.Warning
        }
      }.visibleIf(databricksCliInstalled.not().and(isGettingDatabricksCLIVersion.not()))

      row {
        cell(AsyncProcessIcon("Loading"))
        label(DatabricksBundle.message("settings.cli.version.retrieving"))
      }.visibleIf(isGettingDatabricksCLIVersion)

    }.visibleIf(authTypeProperty.equalsTo(DatabricksConnType.DATABRICKS))

    when (conn.connType) {
      DatabricksConnType.DATABRICKS -> {
        updateDatabricksCLIBlock()
      }
      DatabricksConnType.PROFILE -> {}
      DatabricksConnType.AZURE -> {
        updateAzureCLIBlock()
      }
    }
  }

  private fun authTypeChanged(authType: DatabricksConnType) {
    when (authType) {
      DatabricksConnType.PROFILE -> Unit
      DatabricksConnType.DATABRICKS -> {
        updateDatabricksCLIBlock()
      }
      DatabricksConnType.AZURE -> {
        updateAzureCLIBlock()
      }
    }
  }

  private fun updateDatabricksCLIBlock() {
    isGettingDatabricksCLIVersion.set(true)
    coroutineScope.launch(Dispatchers.IO) {
      val isDatabricksInstalled = DatabricksCliManager.isDatabricksInstalled()
      isGettingDatabricksCLIVersion.set(false)
      coroutineScope.launch(Dispatchers.EDT) {
        databricksCliInstalled.set(isDatabricksInstalled)
      }
    }
  }

  private fun updateAzureCLIBlock() {
    isGettingAzureCLIVersion.set(true)
    coroutineScope.launch(Dispatchers.IO) {
      val azureCLIVersion = AzureCliWrapper.getAzureCLIVersion()
      val account = if (azureCLIVersion != null) {
        AzureCliWrapper.getAzureAccount()
      }
      else {
        null
      }

      isGettingAzureCLIVersion.set(false)

      withContext(Dispatchers.EDT) {
        azureCLIInstalled.set(azureCLIVersion != null)
        azureVersion.set(azureCLIVersion ?: "")

        azureLoggedIn.set(account != null)
        azureUser.set(account ?: "")
      }
    }
  }

  /** Opens config file in the editor. */
  private fun openDatabricksCfg(e: AnActionEvent) {
    val path = "${System.getProperty("user.home")}${File.separator}.databrickscfg"
    val file = LocalFileSystem.getInstance().findFileByPath(path)
    if (file == null) {
      val targetComponent = e.inputEvent?.component as? JComponent ?: return
      val text = "<html>${DatabricksBundle.message("open.profiles.config.error", path)}</html>"
      JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(text, MessageType.INFO) { event ->
        if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
          BrowserUtil.browse(event.url)
        }
      }.setDisposable(uiDisposable).createBalloon().showInCenterOf(targetComponent)
    }
    else {
      FileEditorManagerEx.getInstanceEx(project).openFile(file, true, true)
    }
  }
}