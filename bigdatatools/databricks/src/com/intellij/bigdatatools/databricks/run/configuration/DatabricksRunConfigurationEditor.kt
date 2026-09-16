package com.intellij.bigdatatools.databricks.run.configuration

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.model.ClusterInfoPresentable
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.RadioButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.ui.doOnChange
import com.jetbrains.bigdatatools.common.constants.BdtPluginType
import com.jetbrains.bigdatatools.common.settings.connections.connType
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import java.awt.event.ItemEvent
import java.io.File
import javax.swing.DefaultComboBoxModel
import javax.swing.JList

internal class DatabricksRunConfigurationEditor(private val project: Project) : SettingsEditor<DatabricksRunConfiguration>() {
  private val clusterComboBox = createClusterSelector()
  private val configurationComboBox = createConfigurationsSelector()
  private val fileTextField = createFileSelector()
  private val modeWorkflow = RadioButton(DatabricksRunMode.WORKFLOW.label).apply { isSelected = true }
  private val modeServer = RadioButton(DatabricksRunMode.SERVER.label)

  init {
    if (configurationComboBox.item != null) {
      val clusters = DatabricksDataManager.getInstance(configurationComboBox.item.innerId, project)?.getClusters()?.toTypedArray()
                     ?: emptyArray()
      clusterComboBox.model = DefaultComboBoxModel(clusters)
    }
  }

  private fun createClusterSelector() = ComboBox<ClusterInfoPresentable>(emptyArray()).apply {
    setRenderer(object : SimpleListCellRenderer<ClusterInfoPresentable>() {
      override fun customize(list: JList<out ClusterInfoPresentable>,
                             value: ClusterInfoPresentable?,
                             index: Int,
                             selected: Boolean,
                             hasFocus: Boolean) {
        text = value?.name ?: ""
      }
    })
  }

  private fun createConfigurationsSelector() = ComboBox(getDatabricksConnectionData()).apply {
    addItemListener { event ->
      if (event.stateChange == ItemEvent.SELECTED) {
        val clusters = DatabricksDataManager.getInstance(item.innerId, project)?.getClusters()?.toTypedArray() ?: emptyArray()
        clusterComboBox.model = DefaultComboBoxModel(clusters)
      }
    }
    setRenderer(object : SimpleListCellRenderer<ConnectionData>() {
      override fun customize(list: JList<out ConnectionData>,
                             value: ConnectionData?,
                             index: Int,
                             selected: Boolean,
                             hasFocus: Boolean) {
        text = value?.name ?: ""
      }
    })
  }

  private fun createFileSelector(): TextFieldWithBrowseButton {
    val fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
      .withTitle(DatabricksBundle.message("run.configuration.select.file"))
      .withExtensionFilter(DatabricksBundle.message("run.configuration.select.filter"), "py", "ipynb")
    return TextFieldWithBrowseButton().apply {
      addBrowseFolderListener(project, fileChooserDescriptor)
      textField.doOnChange {
        val isNotebook = File(text).extension.lowercase() == "ipynb"

        modeWorkflow.isEnabled = !isNotebook
        modeServer.isEnabled = !isNotebook

        if (isNotebook) {
          modeWorkflow.isSelected = true
        }
      }
    }
  }

  override fun resetEditorFrom(configuration: DatabricksRunConfiguration) {
    val connectionData = getDatabricksConnectionData().firstOrNull { it.innerId == configuration.configurationId }
    configurationComboBox.selectedItem = connectionData

    val databricksManager = if (connectionData == null) null else DatabricksDataManager.getInstance(connectionData.innerId, project)
    val cluster = databricksManager?.getClusters()?.firstOrNull { it.id == configuration.clusterId }
    clusterComboBox.selectedItem = cluster

    fileTextField.text = configuration.filePath
    modeWorkflow.isSelected = configuration.mode == DatabricksRunMode.WORKFLOW
  }

  override fun applyEditorTo(configuration: DatabricksRunConfiguration) {
    configuration.configurationId = configurationComboBox.item?.innerId ?: ""
    configuration.clusterId = clusterComboBox.item?.id ?: ""
    configuration.filePath = fileTextField.text
    configuration.mode = if (modeWorkflow.isSelected) DatabricksRunMode.WORKFLOW else DatabricksRunMode.SERVER
  }

  override fun createEditor() = panel {
    row(DatabricksBundle.message("run.configuration.connection")) { cell(configurationComboBox).align(AlignX.FILL) }
    row(DatabricksBundle.message("run.configuration.cluster")) { cell(clusterComboBox).align(AlignX.FILL) }
    row(DatabricksBundle.message("run.configuration.file")) { cell(fileTextField).align(AlignX.FILL) }

    buttonsGroup {
      row(DatabricksBundle.message("run.configuration.mode")) { cell(modeWorkflow); cell(modeServer) }
    }
  }

  private fun getDatabricksConnectionData(): Array<ConnectionData> {
    return RfsConnectionDataManager.instance
             ?.getConnections(project)
             ?.filter { it.connType.pluginType == BdtPluginType.DATABRICKS }
             ?.toTypedArray()
           ?: emptyArray<ConnectionData>()
  }
}
