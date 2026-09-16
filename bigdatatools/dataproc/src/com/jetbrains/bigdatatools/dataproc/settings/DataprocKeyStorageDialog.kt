package com.jetbrains.bigdatatools.dataproc.settings

import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.LocalPathCellEditor
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import java.awt.Dimension
import java.awt.event.ActionListener
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable

class DataprocKeyStorageDialog(val project: Project) : DialogWrapper(project, false) {
  private val table = run {
    val columns = arrayOf(KeyNameColumn(), PathColumn())
    val tableModel = createTableModel(columns)
    val tableView = TableView(tableModel)
    tableView.emptyText.text = HdfsMessagesBundle.message("emr.keys.settings.table.empty")
    tableView
  }

  init {
    title = HdfsMessagesBundle.message("emr.key.storage.dialog.title")

    DataprocSshKeysStorage.getInstance().keyMap.forEach {
      table.listTableModel.addRow(TableRow(it.key, it.value))
    }

    init()
  }

  override fun createCenterPanel() = MigPanel().apply {
    add(JLabel(HdfsMessagesBundle.message("emr.keys.settings.label")), CC().gapTop("3").spanX().wrap())
    add(createTableDecorator(table), CC().growX().growY().pushY().spanX().wrap())

    putClientProperty(IS_VISUAL_PADDING_COMPENSATED_ON_COMPONENT_LEVEL_KEY, true)
    border = BorderFactory.createCompoundBorder(JBEmptyBorder(UIUtil.PANEL_REGULAR_INSETS),
                                                IdeBorderFactory.createBorder(SideBorder.BOTTOM))

    minimumSize = Dimension(400, minimumSize.height)
  }

  override fun doOKAction() {
    DataprocSshKeysStorage.getInstance().keyMap = table.listTableModel.items.associate { it.sshKeyName to it.sshKeyPath }.toMutableMap()
    super.doOKAction()
  }

  private fun createTableModel(columns: Array<ColumnInfo<TableRow, Any?>>) = ListTableModel(columns, mutableListOf<TableRow>(), -1)

  private class KeyNameColumn : ColumnInfo<TableRow, Any?>(HdfsMessagesBundle.message("emr.keys.settings.column.key.name")) {
    override fun valueOf(item: TableRow) = item.sshKeyName
    override fun isCellEditable(item: TableRow?): Boolean = true

    override fun setValue(item: TableRow, value: Any?) {
      item.sshKeyName = value?.toString() ?: ""
    }
  }

  private inner class PathColumn : ColumnInfo<TableRow, Any?>(HdfsMessagesBundle.message("emr.keys.settings.column.key.path")) {
    override fun valueOf(item: TableRow): String = item.sshKeyPath
    override fun isCellEditable(item: TableRow?): Boolean = true

    override fun setValue(item: TableRow, value: Any?) {
      item.sshKeyPath = value?.toString() ?: ""
    }

    override fun getEditor(item: TableRow): LocalPathCellEditor = object : LocalPathCellEditor(project) {
      override fun createActionListener(table: JTable): ActionListener = ActionListener {
        val sshKeyPath = item.sshKeyPath
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(if (SystemInfo.isWindows) "ppk" else "pem")

        val prevSelectedFile = if (sshKeyPath.isNotBlank())
          VirtualFileManager.getInstance().refreshAndFindFileByUrl(VfsUtilCore.pathToUrl(sshKeyPath))
        else
          project.guessProjectDir()

        myComponent.childComponent.text = FileChooser.chooseFile(descriptor, project, prevSelectedFile)?.path
      }
    }
  }

  private fun createTableDecorator(tableView: TableView<TableRow>): JPanel {
    val model = tableView.listTableModel

    return ToolbarDecorator.createDecorator(tableView)
      .disableUpAction()
      .disableDownAction()
      .setAddAction {
        model.addRow(TableRow("", ""))
      }.setRemoveAction {
        model.removeRow(tableView.selectedRow)
      }.createPanel()
  }

  private data class TableRow(var sshKeyName: String, var sshKeyPath: String)

  override fun getDimensionServiceKey() = "bigdatatools.aws.emr.keystore.dialog.bounds"
}
