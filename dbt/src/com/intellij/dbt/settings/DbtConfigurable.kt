package com.intellij.dbt.settings

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.database.model.RawDataSource
import com.intellij.database.psi.DataSourceManager
import com.intellij.database.psi.DbDataSource
import com.intellij.database.view.ui.DatabaseConfigEditor
import com.intellij.dbt.DbtBundle
import com.intellij.dbt.dbtSettings
import com.intellij.dbt.modifyDbtModuleEntity
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.MenuItemPresentationFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.popup.ActionPopupOptions
import com.intellij.ui.popup.PopupFactoryImpl.ActionGroupPopup
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import java.awt.Component
import java.io.File
import javax.swing.JComponent
import javax.swing.JTextField

@Suppress("DialogTitleCapitalization")
class DbtConfigurable(private val myModule: Module) : Configurable, Disposable {
  private val dbtPathTextField = TextFieldWithBrowseButton()
  private val moduleEntity = (myModule as? ModuleBridge)?.findModuleEntity(myModule.entityStorage.current)
  private val dbtSettings = moduleEntity?.dbtSettings
  private var dialogDbtExecutablePath = dbtSettings?.dbtExecutablePath ?: ""
  private var dialogDbtDataSourceId = dbtSettings?.dbtDataSourceId ?: ""
  private val dataSourceCombobox = ComboBox<RawDataSource>()

  override fun createComponent(): JComponent {
    if (dbtSettings == null) {
      return panel {
        row {
          label(DbtBundle.message("settings.dbt.project.not.found"))
        }
      }
    }
    myModule.project.messageBus.connect(this).subscribe(DatabaseConfigEditor.TOPIC, DbtDatabaseConfigEditorListener())

    dbtPathTextField.text = dialogDbtExecutablePath
    val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
      .withTitle(DbtBundle.message("settings.choose.dbt.executable.dialog.title.dbt"))
      .withFileFilter { File(it.path).canExecute() }
    val listener = object: ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>(
      dbtPathTextField, myModule.project, descriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
    ) {
      override fun getInitialFile(): VirtualFile? = myModule.guessModuleDir()
    }
    dbtPathTextField.addActionListener(listener)

    val dataSources = DataSourceManager.getManagers(myModule.project).flatMap { it.dataSources }.toTypedArray()
    dataSourceCombobox.addItem(null)
    dataSources.forEach { dataSourceCombobox.addItem(it) }
    dataSourceCombobox.renderer = textListCellRenderer(DbtBundle.message("settings.no.data.source"), RawDataSource::getName)
    dataSourceCombobox.selectedItem = dataSources.firstOrNull { it.uniqueId == dialogDbtDataSourceId }

    return panel {
      row(DbtBundle.message("settings.path.to.dbt.executable")) {
        cell(dbtPathTextField)
          .align(AlignX.FILL)
          .validationInfo {
            validateDbtPath()
          }
      }
      row(DbtBundle.message("settings.data.source")) {
        cell(dataSourceCombobox).align(AlignX.FILL)

        link(DbtBundle.message("settings.add.data.source.label")) { ae ->
          val component = ae.source as Component
          val dataContext = DataManager.getInstance().getDataContext(component)
          val anActionEvent = AnActionEvent.createFromDataContext("dbt database configuration", null, dataContext)
          val actionGroup = ActionManager.getInstance().getAction("DatabaseView.AddDataSourceGroup") as ActionGroup

          val popup = ActionGroupPopup(
            null, null, actionGroup, dataContext,
            ActionPlaces.getActionGroupPopupPlace(anActionEvent.place), MenuItemPresentationFactory(),
            ActionPopupOptions.showDisabled(), null)
          popup.showUnderneathOf(component)
        }
      }
    }.also {
      it.registerValidators(this)
    }
  }

  override fun isModified(): Boolean {
    if (dbtSettings == null) {
      return false
    }
    val dataSourceUniqueId = (dataSourceCombobox.selectedItem as? RawDataSource)?.uniqueId ?: ""
    return dialogDbtExecutablePath != dbtPathTextField.text || dialogDbtDataSourceId != dataSourceUniqueId
  }

  override fun apply() {
    dialogDbtExecutablePath = dbtPathTextField.text
    dialogDbtDataSourceId = (dataSourceCombobox.selectedItem as? RawDataSource)?.uniqueId ?: ""

    ApplicationManager.getApplication().invokeLater {
      ApplicationManager.getApplication().runWriteAction {
        WorkspaceModel.getInstance(myModule.project).updateProjectModel("Update dbt settings") { builder ->
          builder.modifyDbtModuleEntity(dbtSettings!!) {
            dbtExecutablePath = dialogDbtExecutablePath
            dbtDataSourceId = dialogDbtDataSourceId
          }
        }
      }
    }
  }

  override fun getDisplayName(): String {
    @Suppress("DialogTitleCapitalization")
    return DbtBundle.message("dbt.display.name")
  }

  private fun ValidationInfoBuilder.validateDbtPath(): ValidationInfo? {
    val value = dbtPathTextField.text

    if (value.isEmpty()) {
      return error(DbtBundle.message("settings.dbt.path.error.empty"))
    }
    val file = File(value)
    if (!file.exists()) {
      return error(DbtBundle.message("settings.dbt.path.error.do.not.exist"))
    }

    if (!file.canExecute()) {
      return error(DbtBundle.message("settings.dbt.path.error.message.file.not.executable"))
    }
    return null
  }

  override fun reset() {}

  override fun dispose() {
    if (dbtSettings != null && !dbtSettings.reviewed) {
      markAsReviewed()
    }
  }

  override fun disposeUIResources() {
    super.disposeUIResources()
    Disposer.dispose(this)
  }

  private fun markAsReviewed() {
    ApplicationManager.getApplication().invokeLater {
      ApplicationManager.getApplication().runWriteAction {
        WorkspaceModel.getInstance(myModule.project).updateProjectModel("Update dbt settings") { builder ->
          builder.modifyDbtModuleEntity(dbtSettings!!) {
            reviewed = true
          }
        }
      }

      clearFileLevelWarnings("DbtConfigurable.markAsReviewed")
    }
  }

  private fun clearFileLevelWarnings(reason: Any) {
    (DaemonCodeAnalyzerEx.getInstanceEx(myModule.project) as DaemonCodeAnalyzerImpl).restart(reason)
  }

  inner class DbtDatabaseConfigEditorListener : DatabaseConfigEditor.Listener {
    override fun applied(target: Any, isNew: Boolean) {
      if (target is DbDataSource) {
        val dataSource = target.delegateDataSource
        dataSourceCombobox.addItem(dataSource)
        dataSourceCombobox.selectedItem = dataSource
      }
    }
  }
}
