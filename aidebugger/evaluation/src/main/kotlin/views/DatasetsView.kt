package com.intellij.aidebugger.evaluation.views

import com.intellij.aidebugger.common.models.DatasetRow
import com.intellij.aidebugger.evaluation.EvaluationBundle
import com.intellij.aidebugger.evaluation.EvaluationCollector
import com.intellij.aidebugger.evaluation.viewModels.DatasetsViewModel
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.TableUtil
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Path
import java.util.EventObject
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.AbstractCellEditor
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.UIManager
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import kotlin.math.max

private data class InputRow(var input: String, var expected: String)

class DatasetsView(private val project: Project) : JPanel(BorderLayout()) {
    private val vm = project.service<DatasetsViewModel>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.EDT)

    private val inputColumn = object : ColumnInfo<InputRow, String>(EvaluationBundle.message("eval.datasets.column.input")) {
        override fun valueOf(item: InputRow): String = item.input
        override fun isCellEditable(item: InputRow): Boolean = true
        override fun setValue(item: InputRow, value: String) { item.input = value }
    }

    private val expectedColumn = object : ColumnInfo<InputRow, String>(EvaluationBundle.message("eval.datasets.column.expected")) {
        override fun valueOf(item: InputRow): String = item.expected
        override fun isCellEditable(item: InputRow): Boolean = true
        override fun setValue(item: InputRow, value: String) { item.expected = value }
    }

    private val tableModel = ListTableModel<InputRow>(inputColumn, expectedColumn)
    private val table = object : TableView<InputRow>(tableModel) {
        override fun editCellAt(row: Int, column: Int, e: EventObject?): Boolean {
            if (e is MouseEvent && e.clickCount < 2) return false
            clearSelection()
            return super.editCellAt(row, column, e)
        }
    }
    private val ignoreFlag = AtomicBoolean(false)

    private val listModel = DefaultListModel<String>()
    private val datasetList = JBList(listModel)

    init {
        border = JBUI.Borders.empty(8)

        setupTable()
        setupDatasetList()

        val leftPanel = createLeftPanel()
        val rightPanel = createRightPanel()

        val split = OnePixelSplitter(false, 0.25f)
        split.firstComponent = leftPanel
        split.secondComponent = rightPanel

        add(split, BorderLayout.CENTER)

        observeViewModel()
    }

    private fun setupTable() {
        table.emptyText.text = EvaluationBundle.message("eval.datasets.empty.text")
        table.setStriped(true)
        table.setShowGrid(true)
        table.intercellSpacing = Dimension(1, 1)
        table.setRowSelectionAllowed(true)
        table.setColumnSelectionAllowed(false)

        val fm = table.getFontMetrics(table.font)
        val lineHeight = fm.height.coerceAtLeast(16)
        val baseline = lineHeight + table.rowMargin
        table.putClientProperty("eval.baseline.rowHeight", baseline)
        table.rowHeight = baseline * 3

        val renderer = DatasetMultiLineCellRenderer()
        for (i in 0 until table.columnModel.columnCount) {
            table.columnModel.getColumn(i).cellRenderer = renderer
        }

        val editor = DatasetMultiLineCellEditor()
        table.putClientProperty("JTable.autoStartsEdit", false)
        table.setDefaultEditor(String::class.java, editor)
        table.setDefaultEditor(Any::class.java, editor)

        tableModel.addTableModelListener {
            if (ignoreFlag.get()) return@addTableModelListener
            vm.setRows(collectRows())
        }
    }

    private fun setupDatasetList() {
        datasetList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && e.button == MouseEvent.BUTTON1) {
                    val idx = datasetList.locationToIndex(e.point)
                    if (idx >= 0) {
                        datasetList.selectedIndex = idx
                        val currentName = listModel.getElementAt(idx)
                        val newName = Messages.showInputDialog(
                            datasetList,
                            EvaluationBundle.message("eval.datasets.rename.message"),
                            EvaluationBundle.message("eval.datasets.rename.title"),
                            null,
                            currentName,
                            null
                        )?.trim()
                        if (!newName.isNullOrEmpty() && newName != currentName) {
                            val result = vm.renameSelected(newName)
                            if (result == null) {
                                Messages.showErrorDialog(
                                    datasetList,
                                    EvaluationBundle.message("eval.dataset.name.invalidChars"),
                                    EvaluationBundle.message("eval.dataset.name.invalidChars.title")
                                )
                            }
                        }
                    }
                }
            }
        })

        datasetList.addListSelectionListener { ev ->
            if (ev.valueIsAdjusting) return@addListSelectionListener
            val selName = datasetList.selectedValue
            vm.selectDataset(selName)
        }
    }

    @Suppress("DEPRECATION")
    private fun createLeftPanel(): JComponent {
        val decorator = ToolbarDecorator.createDecorator(datasetList)
            .setAddAction {
                EvaluationCollector.reportToolwindowAddDatasetClicked()
                vm.createDataset()
            }
            .setRemoveAction {
                EvaluationCollector.reportToolwindowRemoveDatasetClicked()
                vm.deleteSelected()
            }
            .addExtraAction(object : AnAction(EvaluationBundle.message("eval.datasets.button.duplicate"), null, AllIcons.Actions.Copy) {
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) {
                    EvaluationCollector.reportToolwindowDuplicateDatasetClicked()
                    vm.duplicateSelected()
                }
            })
            .addExtraAction(object : AnAction(EvaluationBundle.message("eval.datasets.button.download"), null, AllIcons.ToolbarDecorator.Export) {
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
                override fun actionPerformed(e: AnActionEvent) {
                    EvaluationCollector.reportToolwindowDownloadDatasetClicked()
                    val saver = FileChooserFactory.getInstance().createSaveFileDialog(
                        FileSaverDescriptor(EvaluationBundle.message("eval.datasets.save.title"), EvaluationBundle.message("eval.datasets.save.description"), "csv", "jsonl", "txt"),
                        project
                    )
                    val defaultName = (vm.selectedName.value ?: "dataset") + ".csv"
                    val result = saver.save(null as Path?, defaultName) ?: return
                    vm.downloadSelected(result.getFile().toPath())
                }
            })
            .disableUpDownActions()
            .createPanel()

        decorator.border = JBUI.Borders.empty()
        return decorator
    }

    private fun createRightPanel(): JComponent {
        val tablePanel = ToolbarDecorator.createDecorator(table)
            .setAddAction { vm.addRowLikeLast() }
            .setRemoveAction {
                TableUtil.removeSelectedItems(table)
                vm.setRows(collectRows())
            }
            .disableUpDownActions()
            .createPanel()

        tablePanel.border = JBUI.Borders.empty()

        val panel = JPanel(BorderLayout())
        panel.add(tablePanel, BorderLayout.CENTER)
        return panel
    }

    private fun collectRows(): List<DatasetRow> =
        tableModel.items.map { row ->
            DatasetRow(
                input = row.input,
                output = row.expected,
                raw = null
            )
        }

    private fun observeViewModel() {
        scope.launch {
            vm.datasetNames.collect { names ->
                listModel.removeAllElements()
                names.forEach { listModel.addElement(it) }
            }
        }

        scope.launch {
            vm.selectedName.collect { selectedName ->
                if (!selectedName.isNullOrBlank()) {
                    runCatching { datasetList.setSelectedValue(selectedName, true) }
                } else if (listModel.size() > 0 && datasetList.selectedIndex < 0) {
                    datasetList.setSelectedIndex(0)
                }
            }
        }

        scope.launch {
            vm.rows.collect { rows ->
                ignoreFlag.set(true)
                val items = rows
                    .map { InputRow(it.input.orEmpty(), it.output.orEmpty()) }
                    .toMutableList()
                runCatching {
                    tableModel.items = items
                    tableModel.fireTableDataChanged()
                }
                ignoreFlag.set(false)
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }
}

private class DatasetMultiLineCellRenderer : JTextArea(), TableCellRenderer {
    init {
        lineWrap = true
        wrapStyleWord = true
        isOpaque = true
        border = null
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val suppressSelection =
            table.isEditing && row == table.editingRow && column != table.editingColumn
        val effectiveSelected = isSelected && !suppressSelection

        val full = value?.toString() ?: ""
        text = full
        font = table.font
        if (effectiveSelected) {
            background = table.selectionBackground
            foreground = table.selectionForeground
        } else {
            background = table.background
            foreground = table.foreground
        }

        val colWidth = try {
            table.columnModel.getColumn(column).width
        } catch (_: Throwable) {
            table.width
        }
        val pad = max(0, table.intercellSpacing.width - 1)
        val width = (colWidth - pad).coerceAtLeast(10)

        setSize(width, Short.MAX_VALUE.toInt())
        val desiredHeight = preferredSize.height
        val rowHeight = table.getRowHeight(row)
        val fits = desiredHeight <= rowHeight + 1

        toolTipText = if (fits) {
            null
        } else {
            makeMultilineTooltip(full, width)
        }

        return this
    }
}

private class DatasetMultiLineCellEditor : AbstractCellEditor(), TableCellEditor {
    private val textArea = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
    }

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        textArea.font = table.font
        textArea.text = value?.toString() ?: ""
        textArea.border = UIManager.getBorder("TextField.border")
        textArea.background = table.background
        textArea.foreground = table.foreground

        val colWidth = try {
            table.columnModel.getColumn(column).width
        } catch (_: Throwable) {
            table.width
        }
        val pad = max(0, table.intercellSpacing.width - 1)
        val width = (colWidth - pad).coerceAtLeast(10)
        val height = table.getRowHeight(row).coerceAtLeast(10)

        textArea.preferredSize = Dimension(width, height)
        textArea.minimumSize = Dimension(width, height)
        textArea.maximumSize = Dimension(Int.MAX_VALUE, height)

        textArea.caretPosition = textArea.document.length

        return textArea
    }

    override fun getCellEditorValue(): Any {
        return textArea.text
    }
}