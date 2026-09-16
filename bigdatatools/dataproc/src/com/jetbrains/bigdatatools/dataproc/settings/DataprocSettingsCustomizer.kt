package com.jetbrains.bigdatatools.dataproc.settings

import com.intellij.bigdatatools.coreUi.settings.CommonSettingsKeys
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.coreUi.settings.ModificationKey
import com.intellij.bigdatatools.coreUi.fields.StringNamedField
import com.intellij.bigdatatools.coreUi.fields.WrappedComponent
import com.intellij.bigdatatools.coreUi.fields.WrappedDropDownList
import com.intellij.bigdatatools.coreUi.settings.withNotEmptyValidator
import com.intellij.bigdatatools.coreUi.ui.row
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.listCellRenderer.listCellRenderer
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.settings.defaultui.SettingsPanelCustomizerExImpl
import com.jetbrains.bigdatatools.dataproc.statistic.DataprocSettingsCollector
import com.jetbrains.bigdatatools.dataproc.util.GcRegion
import com.jetbrains.bigdatatools.gcloud.GCloudUiSettings
import kotlinx.coroutines.CoroutineScope

class DataprocSettingsCustomizer(val project: Project,
                                 connectionData: DataprocConnectionData,
                                 uiDisposable: Disposable,
                                 coroutineScope: CoroutineScope) : SettingsPanelCustomizerExImpl<DataprocConnectionData>() {
  private val gCloudUiComponents = GCloudUiSettings(connectionData, project, uiDisposable, coroutineScope, allowAnonymous = false)

  internal val authTypeChooser = gCloudUiComponents.authTypeChooser
  internal val changeAccount = gCloudUiComponents.changeAccount
  internal val googleProject = gCloudUiComponents.googleProject
  internal val credentialFileChooser = gCloudUiComponents.credentialFileChooser

  internal val nameField = StringNamedField(DataprocConnectionData::name, CommonSettingsKeys.NAME_KEY, connectionData)
    .withNotEmptyValidator(uiDisposable, MessagesBundle.message("validator.nameField"))

  private val render = listCellRenderer<GcRegion> {
    text(value.title)
    text(value.id) {
      foreground = greyForeground
    }
  }

  internal val region = WrappedDropDownList(DataprocConnectionData::region,
                                            GcRegion.entries.toTypedArray(),
                                            ModificationKey(MessagesBundle.message("settings.s3.region")),
                                            connectionData,
                                            defaultValue = GcRegion.europe_central2,
                                            render = render).apply {
    getComponent().enableWidePopup()
  }

  init {
    DataprocSettingsCollector.Util.getInstance().initPanel(this)
  }

  override fun getDefaultComponent(fields: List<WrappedComponent<in DataprocConnectionData>>, conn: DataprocConnectionData) = panel {
    row(nameField)
    row(region)

    gCloudUiComponents.createUi(this)
  }

  override fun getDefaultFields(): List<WrappedComponent<in DataprocConnectionData>> = listOf(nameField,
                                                                                              region,
                                                                                              gCloudUiComponents.authTypeChooser,
                                                                                              gCloudUiComponents.credentialFileChooser,
                                                                                              gCloudUiComponents.googleProject)

  override fun onTestConnectionFinish(createdDriver: Driver?, success: Boolean) {
    gCloudUiComponents.updateState(calledByUser = true)
  }
}