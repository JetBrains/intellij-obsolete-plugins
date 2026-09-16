package com.intellij.bigdatatools.zeppelin.dependency.ui

import com.intellij.bigdatatools.zeppelin.dependency.NoteDependencyManager
import com.intellij.bigdatatools.zeppelin.dependency.model.LibraryResolveStatus
import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinLibrary
import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinLibraryType
import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinUiLibrary
import com.intellij.bigdatatools.zeppelin.dependency.module.ZeppelinModuleUtils
import com.intellij.bigdatatools.zeppelin.interpreter.BaseSettingsDialog
import com.intellij.bigdatatools.zeppelin.models.interpreter.DepType
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency.Companion.MODULE_NAME_PREFIX
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.jarRepository.RepositoryAttachDialog
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ui.configuration.ChooseModulesDialog
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator.createDecorator
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.scale.JBUIScale
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.LocalPathCellEditor
import com.jetbrains.bigdatatools.common.ui.chooser.FileChooserUtil
import com.jetbrains.bigdatatools.common.util.invokeLater
import icons.OpenapiIcons
import java.awt.event.ActionListener
import java.util.function.Consumer
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable

class DependencySettingsDialog(private val dependencyManager: NoteDependencyManager, private val project: Project) : BaseSettingsDialog {
  private val builtinTable = createBuiltinTable()
  private val interpreterTable = createInterpreterTable()
  private val userTable = createUserTable()
  private val sdkTextField = JLabel("")

  init {
    updateUiLibs()
  }

  fun getComponent() = panel {
    panel { // Panel, to avoid vertical alignment fo "SDK:" column with other columns.
      row(ZepMessagesBundle.message("dependency.settings.row.sdk")) { cell(sdkTextField) }
    }
    row(ZepMessagesBundle.message("dependency.settings.row.builtin")) {}
    row { cell(JBScrollPane(builtinTable)).align(Align.FILL).resizableColumn() }.resizableRow()
    row(ZepMessagesBundle.message("dependency.settings.row.interpreter")) {}
    row { cell(JBScrollPane(interpreterTable)).align(Align.FILL).resizableColumn() }.resizableRow()
    row(ZepMessagesBundle.message("dependency.settings.row.user")) {}
    row { cell(createTableDecorator(userTable)).align(Align.FILL).resizableColumn() }.resizableRow()
  }

  private fun getResult(): List<ZeppelinLibrary> {
    val builtinLibs = builtinTable.listTableModel.items.toList()
    val interpreterLibs = interpreterTable.listTableModel.items.toList()
    val userLibs = userTable.listTableModel.items.filter { it.groupArtifactId.isNotBlank() }

    return (builtinLibs + interpreterLibs + userLibs).map { it.getLibrary() }
  }

  private fun addSdkInfo() {
    val sdk = dependencyManager.getSdk(project)
    sdkTextField.text = sdk?.name ?: ZepMessagesBundle.message("dependency.settings.sdk.not.found")
    sdkTextField.icon = if (sdk != null) AllIcons.General.InspectionsOK else AllIcons.General.Error
  }

  private fun getGroupedLibs(): Map<ZeppelinLibraryType, List<ZeppelinUiLibrary>> {
    val libs = dependencyManager.getAllLibraries()

    val uiLibs = libs.map { lib ->
      val status = if (dependencyManager.isLibraryResolved(project, lib))
        LibraryResolveStatus.SUCCESS
      else
        LibraryResolveStatus.FAILED

      ZeppelinUiLibrary.create(lib, status)
    }

    return uiLibs.groupBy { it.type }
  }

  private fun updateUiLibs() {
    val groupedLibs = getGroupedLibs()

    val builtinDeps = groupedLibs[ZeppelinLibraryType.BUILTIN] ?: emptyList()
    val interpreterDeps = groupedLibs[ZeppelinLibraryType.INTERPRETER] ?: emptyList()
    val userDeps = groupedLibs[ZeppelinLibraryType.USER]?.toMutableList() ?: mutableListOf()

    updateLib(builtinTable, builtinDeps)
    updateLib(interpreterTable, interpreterDeps)
    updateLib(userTable, userDeps, asMutable = true)
    addSdkInfo()
  }

  override fun isModified(): Boolean {
    val groupedLibs = getGroupedLibs()
    val userDeps = groupedLibs[ZeppelinLibraryType.USER]?.sortedBy { it.groupArtifactId } ?: emptyList()
    return userTable.listTableModel.items != userDeps
  }

  private fun updateLib(table: TableView<ZeppelinUiLibrary>, newLibs: List<ZeppelinUiLibrary>, asMutable: Boolean = false) {
    table.listTableModel.items = if (asMutable)
      newLibs.sortedBy { it.groupArtifactId }.toMutableList()
    else
      newLibs.sortedBy { it.groupArtifactId }
  }

  private class IconColumn(private val iconsProvider: IconProvider? = null) : ColumnInfo<ZeppelinUiLibrary, Any?>("") {
    companion object {
      val renderer = IconCellRenderer()
    }

    override fun valueOf(item: ZeppelinUiLibrary) = when (item.status) {
      LibraryResolveStatus.SUCCESS -> AllIcons.General.InspectionsOK
      LibraryResolveStatus.FAILED -> AllIcons.General.Error
      LibraryResolveStatus.UNKNOWN -> AllIcons.Nodes.EmptyNode
    }

    override fun getWidth(table: JTable) = JBUIScale.scale(AllIcons.General.Error.iconWidth)
    override fun getRenderer(lib: ZeppelinUiLibrary): IconCellRenderer = renderer.apply { iconProvider = iconsProvider }
  }

  private class ExcludeColumn(val isEditable: Boolean) :
    ColumnInfo<ZeppelinUiLibrary, Any?>(ZepMessagesBundle.message("dependency.settings.column.exclude")) {

    override fun valueOf(item: ZeppelinUiLibrary) = item.excludes.joinToString(",")
    override fun isCellEditable(item: ZeppelinUiLibrary?): Boolean = isEditable

    override fun setValue(item: ZeppelinUiLibrary, value: Any?) {
      val stringValue = value?.toString() ?: ""
      item.excludes = stringValue.split(",").map { it.trim() }
    }
  }

  private inner class PathColumn(val isEditable: Boolean) :
    ColumnInfo<ZeppelinUiLibrary, Any?>(ZepMessagesBundle.message("dependency.settings.column.path")) {
    override fun valueOf(item: ZeppelinUiLibrary): String = item.groupArtifactId
    override fun isCellEditable(item: ZeppelinUiLibrary?): Boolean = isEditable

    override fun setValue(item: ZeppelinUiLibrary, value: Any?) {
      val stringValue = value?.toString() ?: ""
      item.groupArtifactId = stringValue
      item.status = LibraryResolveStatus.UNKNOWN
    }

    override fun getEditor(item: ZeppelinUiLibrary): LocalPathCellEditor = object : LocalPathCellEditor(project) {
      override fun createActionListener(table: JTable): ActionListener = ActionListener {
        val depType = item.getDep().getType()

        // Maven is a special case, because it must have a non-blocking window handler to render correctly on Mac
        if (depType == DepType.MAVEN) {
          showMavenDialog(myComponent.childComponent.text, Consumer { path: String -> myComponent.childComponent.text = path })
          return@ActionListener
        }

        // Now we can process all blocking windows
        val path = when (depType) {
          DepType.FILE -> FileChooserUtil.selectSingleFile(project, myComponent.childComponent.text)?.path
          DepType.MODULE -> showModuleDialog(singleSelection = true).firstOrNull()
          else -> null
        }

        if (null != path) {
          myComponent.childComponent.text = path
        }
      }
    }
  }

  private inner class FromColumn : ColumnInfo<ZeppelinUiLibrary, Any?>(ZepMessagesBundle.message("dependency.settings.column.from")) {
    override fun valueOf(item: ZeppelinUiLibrary): String = item.from
    override fun isCellEditable(item: ZeppelinUiLibrary?): Boolean = false
  }

  private fun createBuiltinTable(): TableView<ZeppelinUiLibrary> {
    val columns = arrayOf(IconColumn(), PathColumn(false))
    val tableModel = createTableModel(columns)
    return TableView(tableModel)
  }

  private fun createInterpreterTable(): TableView<ZeppelinUiLibrary> {
    val columns = arrayOf(IconColumn(), PathColumn(false), ExcludeColumn(false), FromColumn())
    val tableModel = ListTableModel(columns, mutableListOf<ZeppelinUiLibrary>(), -1)
    return TableView(tableModel)
  }

  private fun createUserTable(): TableView<ZeppelinUiLibrary> {
    val columns = arrayOf(IconColumn(), PathColumn(true), ExcludeColumn(true))
    val tableModel = createTableModel(columns)
    val tableView = TableView(tableModel)
    tableView.emptyText.text = ZepMessagesBundle.message("dependency.settings.table.empty")
    return tableView
  }

  private fun createTableModel(columns: Array<ColumnInfo<ZeppelinUiLibrary, Any?>>) =
    ListTableModel(columns, mutableListOf<ZeppelinUiLibrary>(), -1)

  /**
   * Opens maven artifact selection dialog.
   * Uses the consumer to pass a string in format "${desc.groupId}:${desc.artifactId}:${desc.version}" or null.
   */
  private fun showMavenDialog(initialFilter: String? = null, c: Consumer<String>) {
    // Running without invokeLater triggers BDIDE-1134
    invokeLater {
      val repositoryAttachDialog = RepositoryAttachDialog(project, initialFilter, RepositoryAttachDialog.Mode.SEARCH)
      if (repositoryAttachDialog.showAndGet()) {
        val desc = repositoryAttachDialog.selectedLibraryDescriptor
        val result = "${desc.groupId}:${desc.artifactId}:${desc.version}"
        c.accept(result)
      }
    }
  }

  private fun createTableDecorator(tableView: TableView<ZeppelinUiLibrary>): JPanel {
    val model = tableView.listTableModel
    return createDecorator(tableView)
      .disableUpAction()
      .disableDownAction()
      .setAddAction { button ->

        val actionsGroup = DefaultActionGroup()

        actionsGroup.add(DumbAwareAction.create(ZepMessagesBundle.message("dependency.add.maven"), OpenapiIcons.RepositoryLibraryLogo) {
          showMavenDialog(null, Consumer { path: String ->
            model.addRow(ZeppelinUiLibrary(path, emptyList(), ZeppelinLibraryType.USER, LibraryResolveStatus.UNKNOWN))
          })
        })

        actionsGroup.add(DumbAwareAction.create(ZepMessagesBundle.message("dependency.add.module"), AllIcons.Nodes.Module) {
          showModuleDialog(singleSelection = false).forEach { module ->
            model.addRow(ZeppelinUiLibrary(module, emptyList(), ZeppelinLibraryType.USER, LibraryResolveStatus.SUCCESS))
          }
        })

        actionsGroup.add(DumbAwareAction.create(ZepMessagesBundle.message("dependency.add.file"), AllIcons.Nodes.Folder) {
          val fileDescriptor = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor()
          val virtualFiles = FileChooser.chooseFiles(fileDescriptor, project, project.guessProjectDir())

          virtualFiles.forEach { virtualFile ->
            model.addRow(ZeppelinUiLibrary(virtualFile.path, emptyList(), ZeppelinLibraryType.USER, LibraryResolveStatus.UNKNOWN))
          }
        })

        val relativePoint = button.preferredPopupPoint

        ActionManager.getInstance().createActionPopupMenu("ZeppelinDependencySettings", actionsGroup).component.show(
          relativePoint.component,
          relativePoint.point.x,
          relativePoint.point.y)
      }.setRemoveAction {
        model.removeRow(tableView.selectedRow)
      }.createPanel()
  }

  private fun showModuleDialog(singleSelection: Boolean): List<String> {
    val addedModules = userTable.listTableModel.items.filter { it.getDep().isModule() }.map { it.getDep().moduleName }
    val modules = ZeppelinModuleUtils.getModulesNonZeppelin(project, addedModules)

    val dlg = ChooseModulesDialog(project, modules,
                                  ZepMessagesBundle.message("choose.modules.dialog.title"),
                                  ZepMessagesBundle.message("choose.modules.description"))
    if (singleSelection) {
      dlg.setSingleSelectionMode()
    }

    return if (dlg.showAndGet()) {
      dlg.chosenElements.map { "$MODULE_NAME_PREFIX ${it.name}" }
    }
    else {
      emptyList()
    }
  }

  override fun apply(): Boolean {
    dependencyManager.updateUserLibs(getResult())
    return true
  }

  companion object {
    fun showDialog(dependencyManager: NoteDependencyManager, project: Project) = invokeLater {
      if (dependencyManager.isResolving(project)) {
        Messages.showWarningDialog(project,
                                   ZepMessagesBundle.message("dependency.message.resolving.message"),
                                   ZepMessagesBundle.message("dependency.message.resolving.title"))
        return@invokeLater
      }

      val dependencySettingsDialog = DependencySettingsDialog(dependencyManager, project)

      val dialogBuilder = DialogBuilder().apply {
        title(ZepMessagesBundle.message("dependency.settings.title"))
        addOkAction()
        addCancelAction()
        setCenterPanel(dependencySettingsDialog.getComponent())
      }

      if (dialogBuilder.showAndGet()) {
        dependencySettingsDialog.apply()
      }
    }
  }
}
