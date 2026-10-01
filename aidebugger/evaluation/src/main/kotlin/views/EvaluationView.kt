@file:Suppress("DEPRECATION")

package com.intellij.aidebugger.evaluation.views

import androidx.compose.ui.geometry.Rect
import com.intellij.aidebugger.common.models.TracesDatasetsRepository
import com.intellij.aidebugger.common.models.deserializeTraceEventsStateFromHierarchicalStateMap
import com.intellij.aidebugger.common.onboarding.AnchorBus
import com.intellij.aidebugger.common.onboarding.OnboardingAnchorKeys
import com.intellij.aidebugger.common.utility.openTraceToolWindowWithState
import com.intellij.aidebugger.evaluation.EvaluationBundle.message
import com.intellij.aidebugger.evaluation.EvaluationCollector
import com.intellij.aidebugger.evaluation.models.entities.ConfigEvalRow
import com.intellij.aidebugger.evaluation.models.entities.ConfigRow
import com.intellij.aidebugger.evaluation.models.repositories.EvalConfigsRepositoryImpl
import com.intellij.aidebugger.evaluation.models.repositories.EvaluationResultsRepositoryImpl
import com.intellij.aidebugger.evaluation.models.repositories.TableRow
import com.intellij.aidebugger.evaluation.onboarding.OnboardingAnchorRegistry
import com.intellij.aidebugger.evaluation.onboarding.ui.fallbackRect
import com.intellij.aidebugger.evaluation.onboarding.ui.findActionToolbarContainer
import com.intellij.aidebugger.evaluation.onboarding.ui.findPlusButton
import com.intellij.aidebugger.evaluation.onboarding.ui.findRemoteRunButton
import com.intellij.aidebugger.evaluation.onboarding.ui.findRunButton
import com.intellij.aidebugger.evaluation.onboarding.ui.headerRectForColumn
import com.intellij.aidebugger.evaluation.onboarding.ui.ignoreErrors
import com.intellij.aidebugger.evaluation.onboarding.ui.screenRectOrNull
import com.intellij.aidebugger.evaluation.settings.AIToolkitModelsService
import com.intellij.aidebugger.evaluation.settings.AIToolkitSettingsService
import com.intellij.aidebugger.evaluation.viewModels.EvalConfigPopupViewModel
import com.intellij.aidebugger.evaluation.viewModels.EvaluationViewModel
import com.intellij.aidebugger.evaluation.viewModels.RunningAction
import com.intellij.aidebugger.evaluation.views.dialogs.showEvalConfigPopup
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListSelectionModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn
import javax.swing.table.TableRowSorter

private var initialLatestRunShown: Boolean = false
private val LOG = Logger.getInstance(EvaluationView::class.java)

class EvaluationView(private val project: Project) : JPanel(BorderLayout()) {
    private val evalVM = EvaluationViewModel(
        project,
        project.service<EvaluationResultsRepositoryImpl>(),
        project.service<TracesDatasetsRepository>(),
        project.service<EvalConfigsRepositoryImpl>()
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val pageSize = 20
    private var pageIndex: Int = 0

    private var leftToolbarPanel: JComponent? = null
    private var leftTableModel: ListTableModel<ConfigRow>? = null
    private var leftTableView: TableView<ConfigRow>? = null
    private var currentConfigEvaluators: Set<String> = emptySet()

    private var evalModelState: ListTableModel<ConfigEvalRow>? = null
    private var evalTableState: TableView<ConfigEvalRow>? = null
    private var evalScrollPane: JBScrollPane? = null

    private var currentEvaluators: Set<String> = emptySet()
    private var currentEvaluatorsWithExtras: Set<String> = emptySet()
    private var scrollTimer: Timer? = null

    private val onConfigSaved: (String) -> Unit = { configName ->
        evalVM.setSelectedConfigName(configName)
        runCatching {
            evalVM.showLastRunForConfig(configName)
        }
    }

    init {
        border = JBUI.Borders.empty(8)
        val split = OnePixelSplitter(false, 0.35f)
        val leftPanel = createLeftPanel()
        val rightPanel = createRightPanel()
        split.firstComponent = leftPanel
        split.secondComponent = rightPanel
        add(split, BorderLayout.CENTER)
        observeViewModel()

        // If there is an ongoing remote run persisted in project state, resume UI updates
        ApplicationManager.getApplication().invokeLater {
            runCatching { evalVM.resumeRemotePollingIfNeeded() }
        }
    }

    private fun initEvalConfigVM(): EvalConfigPopupViewModel {
        val viewModel = EvalConfigPopupViewModel(
            project,
            project.service<EvalConfigsRepositoryImpl>(),
            project.service<TracesDatasetsRepository>(),
            service<AIToolkitSettingsService>(),
            service<AIToolkitModelsService>()
        )
        viewModel.loadAvailableOptions()
        return viewModel
    }

    private fun createLeftPanel(): JComponent {
        val nameCol = object : ColumnInfo<ConfigRow, String>(message("eval.view.column.name")) {
            override fun valueOf(item: ConfigRow): String = item.name
            override fun isCellEditable(item: ConfigRow) = false
        }
        val dsCol = object : ColumnInfo<ConfigRow, String>(message("eval.view.column.dataset")) {
            override fun valueOf(item: ConfigRow): String = item.dataset
            override fun isCellEditable(item: ConfigRow) = false
        }
        val itemsCol = object : ColumnInfo<ConfigRow, String>(message("eval.view.column.items")) {
            override fun valueOf(item: ConfigRow): String = item.items
            override fun isCellEditable(item: ConfigRow) = false
        }
        val avgTimeCol = object : ColumnInfo<ConfigRow, String>(message("eval.view.column.avg.time")) {
            override fun valueOf(item: ConfigRow): String = item.avgAgentTimeSec
            override fun isCellEditable(item: ConfigRow) = false
        }
        val avgTokensCol = object : ColumnInfo<ConfigRow, String>(message("eval.view.column.avg.tokens")) {
            override fun valueOf(item: ConfigRow): String = item.avgAgentTokens
            override fun isCellEditable(item: ConfigRow) = false
        }
        val lastRunCol = object : ColumnInfo<ConfigRow, String>(message("eval.view.column.latest.run")) {
            override fun valueOf(item: ConfigRow): String = item.latestRun
            override fun isCellEditable(item: ConfigRow) = false
        }

        val tableModel = ListTableModel<ConfigRow>(nameCol, dsCol, itemsCol, avgTimeCol, avgTokensCol, lastRunCol)
        val table = object : TableView<ConfigRow>(tableModel) {
            override fun prepareRenderer(
                renderer: TableCellRenderer,
                row: Int,
                column: Int
            ): Component {
                val c = super.prepareRenderer(renderer, row, column)

                val running = evalVM.isRunning.value || evalVM.runningAction.value != RunningAction.NONE
                val selectedName = evalVM.getSelectedConfigName()
                val modelRow = convertRowIndexToModel(row)
                val item = listTableModel.getItem(modelRow)
                val isSelected = isRowSelected(row)

                if (running) {
                    val isActiveConfig = !selectedName.isNullOrBlank() && item != null && item.name == selectedName
                    if (isActiveConfig) {
                        val baseBg = if (isSelected) UIUtil.getPanelBackground() else UIUtil.getTableBackground()
                        c.background = baseBg.darker()
                        c.foreground = UIUtil.getLabelDisabledForeground()
                    } else {
                        val baseBg = if (isSelected) UIUtil.getPanelBackground() else UIUtil.getTableBackground()
                        c.background = baseBg
                        c.foreground = UIUtil.getLabelDisabledForeground()
                    }
                } else {
                    if (isSelected) {
                        c.background = UIUtil.getTableSelectionBackground()
                        c.foreground = UIUtil.getTableSelectionForeground()
                    } else {
                        c.background = UIUtil.getTableBackground()
                        c.foreground = UIUtil.getTableForeground()
                    }
                }

                return c
            }
        }
        leftTableModel = tableModel
        leftTableView = table

        table.emptyText.text = message("eval.view.empty.configs")
        table.setStriped(true)
        table.setShowGrid(false)
        table.border = null
        table.tableHeader.border = null
        table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        setTwoLineHeaderHeight(table)
        installTwoLineEllipsizingHeader(table)

        table.selectionModel = object : DefaultListSelectionModel() {
            override fun setSelectionInterval(index0: Int, index1: Int) {
                if (evalVM.isRunning.value) return else super.setSelectionInterval(index0, index1)
            }

            override fun addSelectionInterval(index0: Int, index1: Int) {
                if (evalVM.isRunning.value) return else super.addSelectionInterval(index0, index1)
            }
        }

        fun refreshTable(selectName: String? = null) {
            evalVM.refreshConfigList()
            if (selectName != null) {
                val pos = evalVM.getConfigIndexByName(selectName)
                if (pos >= 0) {
                    pageIndex = pos / pageSize
                }
            }
        }

        val leftPanel = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                EvaluationCollector.reportToolwindowAddEvalConfigurationClicked(project)
                val evalConfigVM = initEvalConfigVM()
                showEvalConfigPopup(project, evalConfigVM,  null) { savedName ->
                    refreshTable(savedName)
                    onConfigSaved(savedName)
                }
            }
            .setAddActionUpdater {
                !evalVM.isRunning.value
            }
            .setRemoveAction {
                EvaluationCollector.reportToolwindowRemoveEvalConfigurationClicked(project)
                val idx = table.selectedRow
                if (idx < 0) return@setRemoveAction
                val allConfigs = evalVM.configRows.value
                val deletedGlobalIdx = pageIndex * pageSize + idx
                if (deletedGlobalIdx !in allConfigs.indices) return@setRemoveAction
                val row = allConfigs[deletedGlobalIdx]
                evalVM.deleteConfig(row.name)
                val remaining = evalVM.listConfigs()
                if (remaining.isNotEmpty()) {
                    val selectGlobalIdx = deletedGlobalIdx.coerceAtMost(remaining.size - 1)
                    val selectName = remaining.getOrNull(selectGlobalIdx)?.name
                    if (selectName != null) {
                        refreshTable(selectName)
                    } else {
                        refreshTable()
                    }
                } else {
                    refreshTable()
                    evalVM.setSelectedConfigName("")
                }
            }
            .setRemoveActionUpdater {
                !evalVM.isRunning.value
            }
            // TODO: (@gas) search in the monorepo for solution
            .addExtraAction(object : AnAction(message("eval.view.button.edit"), null, AllIcons.Actions.Edit) {
                override fun actionPerformed(e: AnActionEvent) {
                    EvaluationCollector.reportToolwindowEditEvalConfigurationClicked(project)
                    val idx = table.selectedRow
                    if (idx >= 0) {
                        val allConfigs = evalVM.configRows.value
                        val globalIdx = pageIndex * pageSize + idx
                        if (globalIdx in allConfigs.indices) {
                            val row = allConfigs[globalIdx]
                            val info = evalVM.listConfigs().firstOrNull { it.name == row.name }
                            if (info != null) {
                                val evalConfigVM = initEvalConfigVM()
                                showEvalConfigPopup(project, evalConfigVM,  info) { savedName ->
                                    refreshTable(savedName)
                                    onConfigSaved(savedName)
                                }
                            }
                        }
                    }
                }

                override fun getActionUpdateThread() = ActionUpdateThread.EDT

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = !evalVM.isRunning.value
                }
            })
            .addExtraAction(object : AnAction(message("eval.view.button.duplicate"), null, AllIcons.Actions.Copy) {
                override fun actionPerformed(e: AnActionEvent) {
                    EvaluationCollector.reportToolwindowDuplicateEvalConfigurationClicked(project)
                    val idx = table.selectedRow
                    if (idx < 0) return
                    val allConfigs = evalVM.configRows.value
                    val globalIdx = pageIndex * pageSize + idx
                    if (globalIdx !in allConfigs.indices) return
                    val row = allConfigs[globalIdx]
                    val newInfo = evalVM.duplicateConfig(row.name)
                    if (newInfo != null) {
                        refreshTable(newInfo.name)
                        onConfigSaved(newInfo.name)
                    }
                }

                override fun getActionUpdateThread() = ActionUpdateThread.EDT

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = !evalVM.isRunning.value
                }
            })
            .addExtraAction(object : AnAction(message("eval.view.button.run"), null, AllIcons.Actions.Execute) {
                override fun actionPerformed(e: AnActionEvent) {
                    when (evalVM.runningAction.value) {
                        RunningAction.TRACER -> EvaluationCollector.reportToolwindowCancelEvalConfigurationClicked()
                        else -> EvaluationCollector.reportToolwindowRunEvalConfigurationClicked(project)
                    }
                    try {
                        AnchorBus.sink?.setFlag(OnboardingAnchorKeys.EVALUATION_VIEW_RUN_BUTTON_PRESSED, true)
                    } catch (_: Throwable) { }
                    evalVM.runEvaluation()
                }

                override fun getActionUpdateThread() = ActionUpdateThread.EDT

                override fun update(e: AnActionEvent) {
                    val current = evalVM.runningAction.value
                    when (current) {
                        RunningAction.REMOTE -> {
                            e.presentation.icon = AllIcons.Actions.Execute
                            e.presentation.isEnabled = false
                            e.presentation.text = message("eval.view.button.run")
                        }
                        RunningAction.EVAL,
                        RunningAction.TRACER -> {
                            e.presentation.icon = AllIcons.Debugger.KillProcess
                            e.presentation.text = message("eval.view.button.stop")
                            e.presentation.isEnabled = true
                        }
                        else -> {
                            e.presentation.icon = AllIcons.Actions.Execute
                            e.presentation.isEnabled = true
                            e.presentation.text = message("eval.view.button.run")
                        }
                    }
                }
            })
            .addExtraAction(object : AnAction(message("eval.view.button.remote.run"), null, AllIcons.Actions.RunAll) {
                override fun actionPerformed(e: AnActionEvent) {
                    EvaluationCollector.reportToolwindowRemoteRunEvalConfigurationClicked(project)
                    try {
                        AnchorBus.sink?.setFlag(OnboardingAnchorKeys.EVALUATION_VIEW_REMOTE_RUN_BUTTON_PRESSED, true)
                    } catch (_: Throwable) { }
                    evalVM.runRemoteEvaluation()
                }

                override fun getActionUpdateThread() = ActionUpdateThread.EDT

                override fun update(e: AnActionEvent) {
                    val current = evalVM.runningAction.value
                    when (current) {
                        RunningAction.REMOTE -> {
                            e.presentation.icon = AllIcons.Debugger.KillProcess
                            e.presentation.text = message("eval.view.button.stop.remote.run")
                            e.presentation.isEnabled = true
                        }
                        RunningAction.EVAL,
                        RunningAction.TRACER -> {
                            e.presentation.isEnabled = false
                            e.presentation.icon = AllIcons.Actions.RunAll
                            e.presentation.text = message("eval.view.button.remote.run")
                        }
                        else -> {
                            e.presentation.icon = AllIcons.Actions.RunAll
                            e.presentation.isEnabled = true
                            e.presentation.text = message("eval.view.button.remote.run")
                        }
                    }
                }
            })
            .addExtraAction(object : AnAction("", null, AnimatedIcon.Default()) {
                override fun actionPerformed(e: AnActionEvent) {
                }

                override fun getActionUpdateThread() = ActionUpdateThread.EDT

                override fun update(e: AnActionEvent) {
                    val running = evalVM.isRunning.value
                    e.presentation.isEnabled = true
                    e.presentation.isVisible = running
                }
            })
            .disableUpDownActions()
            .createPanel()

        UIUtil.findComponentOfType(leftPanel, JScrollPane::class.java)?.apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        }

        leftPanel.border = JBUI.Borders.empty()
        leftToolbarPanel = leftPanel


        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    if (evalVM.isRunning.value) return
                    val row = table.selectedRow
                    if (row >= 0) {
                        val allConfigs = evalVM.configRows.value
                        val globalIdx = pageIndex * pageSize + row
                        if (globalIdx in allConfigs.indices) {
                            val configRow = allConfigs[globalIdx]
                            val info = evalVM.listConfigs().firstOrNull { it.name == configRow.name }
                            if (info != null) {
                                val evalConfigVM = initEvalConfigVM()
                                showEvalConfigPopup(project, evalConfigVM, info) { savedName ->
                                    refreshTable(savedName)
                                    onConfigSaved(savedName)
                                }
                            }
                        }
                    }
                }
            }
        })

        table.selectionModel.addListSelectionListener { ev ->
            if (ev.valueIsAdjusting) return@addListSelectionListener
            val idx = table.selectedRow
            if (idx >= 0) {
                val allConfigs = evalVM.configRows.value
                val globalIdx = pageIndex * pageSize + idx
                if (globalIdx in allConfigs.indices) {
                    val configRow = allConfigs[globalIdx]
                    val prevName = evalVM.getSelectedConfigName()
                    evalVM.setSelectedConfigName(configRow.name)
                    try {
                        val notRunning = !evalVM.isRunning.value && evalVM.runningAction.value == RunningAction.NONE
                        if (notRunning) {
                            val hasAnyData =
                                evalVM.tableIds.value.isNotEmpty() || evalVM.tableRows.value.isNotEmpty() || evalVM.tableOutputs.value.isNotEmpty()
                            if (prevName != configRow.name) {
                                evalVM.showLastRunForConfig(configRow.name)
                            } else if (!hasAnyData) {
                                evalVM.showLastRunForConfig(configRow.name)
                            }
                        }
                    } catch (e: Throwable) {
                        LOG.warn("Failed to load last run for config: ${configRow.name}", e)
                    }
                }
            }
        }

        refreshTable()
        try {
            val notRunning = !evalVM.isRunning.value && evalVM.runningAction.value == RunningAction.NONE
            val hasAnyData =
                evalVM.tableIds.value.isNotEmpty() || evalVM.tableRows.value.isNotEmpty() || evalVM.tableOutputs.value.isNotEmpty()
            if (!initialLatestRunShown && notRunning && !hasAnyData) {
                if (tableModel.rowCount > 0) {
                    table.selectionModel.setSelectionInterval(0, 0)
                }
                initialLatestRunShown = true
            } else {
                val saved = evalVM.getSelectedConfigName()
                if (!saved.isNullOrBlank() && notRunning && !hasAnyData) {
                    evalVM.showLastRunForConfig(saved)
                }
            }
        } catch (e: Throwable) {
            LOG.warn("Failed to initialize table selection", e)
        }

        updateConfigTableEnabled()

        registerOnboardingAnchorsForToolbar(leftPanel)

        return leftPanel
    }

    private fun createRightPanel(): JComponent {
        val evaluatorNames = evalVM.tableRows.value.flatMap { it.evaluatorScores.keys }.toSet().sorted()
        currentEvaluators = evaluatorNames.toSet()
        currentEvaluatorsWithExtras =
            evalVM.tableRows.value.flatMap { it.evaluatorExtras.keys }.toSet()

        val columns = mutableListOf<ColumnInfo<ConfigEvalRow, String>>()

        columns.add(object : ColumnInfo<ConfigEvalRow, String>(message("eval.view.column.id")) {
            override fun valueOf(item: ConfigEvalRow): String = item.id
            override fun isCellEditable(item: ConfigEvalRow) = false
        })
        columns.add(object : ColumnInfo<ConfigEvalRow, String>(message("eval.view.column.input")) {
            override fun valueOf(item: ConfigEvalRow): String = item.input
            override fun isCellEditable(item: ConfigEvalRow) = false
        })
        columns.add(object : ColumnInfo<ConfigEvalRow, String>(message("eval.view.column.output")) {
            override fun valueOf(item: ConfigEvalRow): String = item.output
            override fun isCellEditable(item: ConfigEvalRow) = false
        })

        for (evaluatorName in evaluatorNames) {
            columns.add(object : ColumnInfo<ConfigEvalRow, String>(message("eval.view.column.score", evaluatorName)) {
                override fun valueOf(item: ConfigEvalRow): String {
                    return item.evaluatorScores[evaluatorName] ?: ""
                }
                override fun isCellEditable(item: ConfigEvalRow) = false
            })

            if (evalVM.tableRows.value.any { it.evaluatorExtras.containsKey(evaluatorName) }) {
                columns.add(object : ColumnInfo<ConfigEvalRow, String>(message("eval.view.column.extra", evaluatorName)) {
                    override fun valueOf(item: ConfigEvalRow): String {
                        return item.evaluatorExtras[evaluatorName] ?: ""
                    }
                    override fun isCellEditable(item: ConfigEvalRow) = false
                })
            }
        }

        val model = ListTableModel<ConfigEvalRow>(*columns.toTypedArray())
        val count = maxOf(
            evalVM.tableIds.value.size,
            evalVM.tableRows.value.size,
            evalVM.tableOutputs.value.size
        )
        val items = (0 until count).map { idx ->
            val row =
                evalVM.tableRows.value.getOrElse(idx) { TableRow("", emptyMap(), emptyMap()) }
            val output = evalVM.tableOutputs.value.getOrElse(idx) { "" }
            val id = (idx + 1).toString()
            ConfigEvalRow(id, row.input, output, row.evaluatorScores, row.evaluatorExtras)
        }
        model.setItems(items)

        val table = TableView<ConfigEvalRow>(model)
        evalModelState = model
        evalTableState = table

        table.selectionModel = object : DefaultListSelectionModel() {
            override fun setSelectionInterval(index0: Int, index1: Int) {
                if (evalVM.isRunning.value) return else super.setSelectionInterval(index0, index1)
            }
            override fun addSelectionInterval(index0: Int, index1: Int) {
                if (evalVM.isRunning.value) return else super.addSelectionInterval(index0, index1)
            }
        }
        table.emptyText.text = message("eval.view.empty.results")
        table.setStriped(true)

        table.setShowGrid(true)
        table.intercellSpacing = Dimension(1, 1)
        table.border = null
        table.tableHeader.border = null

        try {
            val sorter: TableRowSorter<ListTableModel<ConfigEvalRow>> = TableRowSorter(model)
            for (colIdx in 0 until model.columnCount) {
                val colName = model.getColumnName(colIdx)
                if (colName.endsWith(" Score")) {
                    sorter.setComparator(colIdx) { a, b ->
                        fun parse(x: Any?): Double? =
                            when (x) {
                                null -> null
                                is Number -> x.toDouble()
                                is String -> x.trim().toDoubleOrNull()
                                else -> x.toString().toDoubleOrNull()
                            }

                        val da = parse(a)
                        val db = parse(b)
                        when {
                            da != null && db != null -> da.compareTo(db)
                            da != null -> 1
                            db != null -> -1
                            else -> a.toString().compareTo(b.toString(), ignoreCase = true)
                        }
                    }
                }
            }
            table.rowSorter = sorter
        } catch (e: Exception) {
            LOG.warn("Failed to configure table sorter", e)
        }

        val scroll = JBScrollPane(table)
        evalScrollPane = scroll
        scroll.border = null
        scroll.viewportBorder = null
        scroll.isOpaque = false
        scroll.viewport.isOpaque = false
        scroll.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scroll.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED

        adjustEvalResultColumnWidths(table)
        adjustTableAutoResizeMode(table, scroll)
        enableEvalResultsMultiline(table, scroll)
        enableEvalResultsTopLeftAlignment(table)

        scroll.addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent?) {
                try {
                    adjustTableAutoResizeMode(table, scroll)
                } catch (ex: Throwable) {
                    LOG.warn("Failed to adjust table resize mode", ex)
                }
            }
        })

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    if (evalVM.isRunning.value) return
                    val viewRow = table.selectedRow
                    if (viewRow >= 0) {
                        val currentModel = evalModelState ?: return
                        val modelRow = try {
                            if (table.rowSorter != null) table.convertRowIndexToModel(viewRow) else viewRow
                        } catch (ex: Throwable) {
                            LOG.warn("Failed to convert row index", ex)
                            viewRow
                        }
                        val hasOutput = try {
                            (currentModel.getValueAt(modelRow, 2) as? String)?.isNotBlank() == true
                        } catch (ex: Throwable) {
                            LOG.warn("Failed to get output value", ex)
                            false
                        }
                        if (!hasOutput) return
                        val configName = evalVM.getSelectedConfigName() ?: return
                        val dps = evalVM.getOrLoadDataPoints(configName) ?: return
                        val dp = dps.getOrNull(modelRow)
                        val finalDp = dp ?: run {
                            val inputVal = try {
                                (currentModel.getValueAt(modelRow, 1) as? String)?.trim()
                            } catch (ex: Throwable) {
                                LOG.warn("Failed to get input value", ex)
                                null
                            }
                            val outputVal = try {
                                (currentModel.getValueAt(modelRow, 2) as? String)?.trim()
                            } catch (ex: Throwable) {
                                LOG.warn("Failed to get output value for matching", ex)
                                null
                            }
                            when {
                                inputVal != null -> {
                                    val byInput = dps.filter { it.input?.trim() == inputVal }
                                    when {
                                        byInput.isEmpty() -> null
                                        byInput.size == 1 -> byInput.first()
                                        else -> byInput.firstOrNull {
                                            (it.outputGen ?: "").trim() == (outputVal ?: "")
                                        } ?: byInput.first()
                                    }
                                }
                                else -> null
                            }
                        }
                        finalDp?.let {
                            val traceEventsState =
                                deserializeTraceEventsStateFromHierarchicalStateMap(it.raw)
                            openTraceToolWindowWithState(project, traceEventsState)
                        }
                    }
                }
            }
        })

        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty()
        panel.add(scroll, BorderLayout.CENTER)

        registerOnboardingAnchorForOutputs(table)

        return panel
    }

    private fun registerOnboardingAnchorsForToolbar(panel: JPanel) {
        try {
            val header = findActionToolbarContainer(panel) ?: panel

            installAnchorProvider(
                key = OnboardingAnchorKeys.EVALUATION_VIEW_PLUS_BUTTON,
                resolver = { findPlusButton(header) ?: header },
                fallback = { fallbackRect(panel) }
            )

            installAnchorProvider(
                key = OnboardingAnchorKeys.EVALUATION_VIEW_RUN_BUTTON,
                resolver = { findRunButton(header) ?: header },
                fallback = { fallbackRect(panel) }
            )

            installAnchorProvider(
                key = OnboardingAnchorKeys.EVALUATION_VIEW_REMOTE_RUN_BUTTON,
                resolver = { findRemoteRunButton(header) ?: header },
                fallback = { fallbackRect(panel) }
            )
        } catch (_: Throwable) { }
    }

    private fun registerOnboardingAnchorForOutputs(table: TableView<ConfigEvalRow>) {
        try {
            installAnchorProvider(
                key = OnboardingAnchorKeys.EVALUATION_VIEW_RESULTS_OUTPUT_COLUMN,
                resolver = { table.headerRectForColumn("Output")?.let { null }; table.tableHeader },
                fallback = { fallbackRect(table) }
            )
        } catch (_: Throwable) { }
    }

    private fun observeViewModel() {
        scope.launch {
            evalVM.isRunning.collectLatest {
                refreshToolbarActions()
                updateConfigTableEnabled()
            }
        }
        scope.launch {
            evalVM.runningAction.collectLatest {
                refreshToolbarActions()
                updateConfigTableEnabled()
            }
        }
        scope.launch {
            evalVM.statusText.collectLatest { status ->
                refreshToolbarActions()
                val txt = status.orEmpty()
                if (txt.contains("evaluat", ignoreCase = true)) {
                    val app = ApplicationManager.getApplication()
                    val runnable = Runnable {
                        evalTableState?.let { table ->
                            try {
                                adjustEvalResultColumnWidths(table)
                            } catch (e: Throwable) {
                                LOG.warn("Failed to adjust column widths", e)
                            }
                            try {
                                val cols = getTextColumnIndices(table)
                                updateVisibleRowHeights(table, cols)
                            } catch (e: Throwable) {
                                LOG.warn("Failed to update row heights", e)
                            }
                            table.revalidate()
                            table.repaint()
                            table.parent?.revalidate()
                            table.parent?.repaint()
                        }
                    }
                    if (app.isDispatchThread) runnable.run() else app.invokeLater(runnable)
                }
            }
        }
        scope.launch {
            evalVM.configRows.collectLatest { rows ->
                val model = leftTableModel
                val table = leftTableView
                if (model != null && table != null) {
                    val from = (pageIndex * pageSize).coerceAtMost(rows.size)
                    val to = ((pageIndex + 1) * pageSize).coerceAtMost(rows.size)
                    val page = if (from < to) rows.subList(from, to) else emptyList()

                    val allEvaluators = rows.flatMap { it.aggregatedScores.keys }.toSet()
                    val evaluatorsChanged = allEvaluators != currentConfigEvaluators

                    val app = ApplicationManager.getApplication()
                    val runnable = Runnable {
                        if (evaluatorsChanged) {
                            currentConfigEvaluators = allEvaluators
                            val sortedEvaluators = allEvaluators.sorted()

                            val startColumns = listOf(
                                object : ColumnInfo<ConfigRow, String>(message("eval.view.column.name")) {
                                    override fun valueOf(item: ConfigRow): String = item.name
                                    override fun isCellEditable(item: ConfigRow) = false
                                    override fun getPreferredStringValue(): String = "Configuration Name Placeholder"
                                },
                                object : ColumnInfo<ConfigRow, String>(message("eval.view.column.dataset")) {
                                    override fun valueOf(item: ConfigRow): String = item.dataset
                                    override fun isCellEditable(item: ConfigRow) = false
                                    override fun getPreferredStringValue(): String = "Dataset Name Placeholder"
                                },
                                object : ColumnInfo<ConfigRow, String>(message("eval.view.column.items")) {
                                    override fun valueOf(item: ConfigRow): String = item.items
                                    override fun isCellEditable(item: ConfigRow) = false
                                    override fun getPreferredStringValue(): String = "1000"
                                },
                                object : ColumnInfo<ConfigRow, String>(message("eval.view.column.avg.time")) {
                                    override fun valueOf(item: ConfigRow): String = item.avgAgentTimeSec
                                    override fun isCellEditable(item: ConfigRow) = false
                                    override fun getPreferredStringValue(): String = "00.00"
                                },
                                object : ColumnInfo<ConfigRow, String>(message("eval.view.column.avg.tokens")) {
                                    override fun valueOf(item: ConfigRow): String = item.avgAgentTokens
                                    override fun isCellEditable(item: ConfigRow) = false
                                    override fun getPreferredStringValue(): String = "0000"
                                }
                            )

                            val endColumns = listOf(
                                object : ColumnInfo<ConfigRow, String>(message("eval.view.column.latest.run")) {
                                    override fun valueOf(item: ConfigRow): String = item.latestRun
                                    override fun isCellEditable(item: ConfigRow) = false
                                    override fun getPreferredStringValue(): String = "2023-01-01 12:00"
                                }
                            )

                            val dynamicColumns = sortedEvaluators.map { evalName ->
                                object : ColumnInfo<ConfigRow, String>(message("eval.view.column.averaged", evalName)) {
                                    override fun valueOf(item: ConfigRow): String = item.aggregatedScores[evalName] ?: ""
                                    override fun isCellEditable(item: ConfigRow) = false
                                    override fun getPreferredStringValue(): String = "0.00"
                                }
                            }

                            val newModel = ListTableModel<ConfigRow>(*(startColumns + dynamicColumns + endColumns).toTypedArray())
                            newModel.items = page.toMutableList()
                            leftTableModel = newModel
                            table.setModelAndUpdateColumns(newModel)
                            table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
                        } else {
                            model.items = page.toMutableList()
                            model.fireTableDataChanged()
                        }
                        adjustColumnWidths(table)

                        val saved = evalVM.getSelectedConfigName()
                        if (page.isNotEmpty() && !saved.isNullOrBlank()) {
                            val idx = page.indexOfFirst { it.name == saved }
                            if (idx >= 0) {
                                table.selectionModel.setSelectionInterval(idx, idx)
                            } else if (page.isNotEmpty()) {
                                table.selectionModel.setSelectionInterval(0, 0)
                            }
                        } else if (page.isNotEmpty()) {
                            table.selectionModel.setSelectionInterval(0, 0)
                        }
                    }
                    if (app.isDispatchThread) runnable.run() else app.invokeLater(runnable)
                }
            }
        }
        scope.launch {
            evalVM.tableIds.collectLatest {
                refreshEvalTable()
            }
        }
        scope.launch {
            evalVM.tableRows.collectLatest {
                refreshEvalTable()
            }
        }
        scope.launch {
            evalVM.tableOutputs.collectLatest {
                refreshEvalTable()
            }
        }
    }

    private fun refreshToolbarActions() {
        val app = ApplicationManager.getApplication()
        val runnable = Runnable {
            leftToolbarPanel?.let { panel ->
                try {
                    findActionToolbarIn(panel)?.updateActionsImmediately()
                } catch (e: Throwable) {
                    LOG.warn("Failed to update toolbar actions", e)
                }
                panel.revalidate()
                panel.repaint()
            }
        }
        if (app.isDispatchThread) runnable.run() else app.invokeLater(runnable)
    }

    private fun updateConfigTableEnabled() {
        val table = leftTableView ?: return
        val app = ApplicationManager.getApplication()
        val runnable = Runnable {
            table.repaint()
            table.revalidate()
        }
        if (app.isDispatchThread) runnable.run() else app.invokeLater(runnable)
    }

private fun installAnchorProvider(
    key: String,
    resolver: () -> Component?,
    fallback: () -> Rect?
) = ignoreErrors {
    OnboardingAnchorRegistry.setProvider(key) {
        resolver()?.screenRectOrNull() ?: fallback()
    }

    SwingUtilities.invokeLater {
        resolver()?.screenRectOrNull()?.let { rect ->
            AnchorBus.sink?.set(key, rect)
        }
    }
}
    private fun refreshEvalTable() {
        val table = evalTableState ?: return
        val scroll = evalScrollPane
        val model = evalModelState ?: return

        val app = ApplicationManager.getApplication()
        val runnable = Runnable {
            var activeModel = model
            val newEvaluators = evalVM.tableRows.value.flatMap { it.evaluatorScores.keys }.toSet()
            val newEvaluatorsWithExtras =
                evalVM.tableRows.value.flatMap { it.evaluatorExtras.keys }.toSet()
            val evaluatorsChanged = newEvaluators != currentEvaluators
            val extrasChanged = newEvaluatorsWithExtras != currentEvaluatorsWithExtras

            if (evaluatorsChanged || extrasChanged) {
                currentEvaluators = newEvaluators
                currentEvaluatorsWithExtras = newEvaluatorsWithExtras

                val evaluatorNames = newEvaluators.sorted()
                val columns = mutableListOf<ColumnInfo<ConfigEvalRow, String>>()

                columns.add(object : ColumnInfo<ConfigEvalRow, String>(message("eval.view.column.id")) {
                    override fun valueOf(item: ConfigEvalRow): String = item.id
                    override fun isCellEditable(item: ConfigEvalRow) = false
                })
                columns.add(object : ColumnInfo<ConfigEvalRow, String>(message("eval.view.column.input")) {
                    override fun valueOf(item: ConfigEvalRow): String = item.input
                    override fun isCellEditable(item: ConfigEvalRow) = false
                })
                columns.add(object : ColumnInfo<ConfigEvalRow, String>(message("eval.view.column.output")) {
                    override fun valueOf(item: ConfigEvalRow): String = item.output
                    override fun isCellEditable(item: ConfigEvalRow) = false
                })

                for (evaluatorName in evaluatorNames) {
                    columns.add(object : ColumnInfo<ConfigEvalRow, String>(message("eval.view.column.score", evaluatorName)) {
                        override fun valueOf(item: ConfigEvalRow): String {
                            return item.evaluatorScores[evaluatorName] ?: ""
                        }

                        override fun isCellEditable(item: ConfigEvalRow) = false
                    })

                    if (evalVM.tableRows.value.any { it.evaluatorExtras.containsKey(evaluatorName) }) {
                        columns.add(object : ColumnInfo<ConfigEvalRow, String>(message("eval.view.column.extra", evaluatorName)) {
                            override fun valueOf(item: ConfigEvalRow): String {
                                return item.evaluatorExtras[evaluatorName] ?: ""
                            }

                            override fun isCellEditable(item: ConfigEvalRow) = false
                        })
                    }
                }

                val newModel = ListTableModel<ConfigEvalRow>(*columns.toTypedArray())
                evalModelState = newModel
                activeModel = newModel
                try {
                    table.setModelAndUpdateColumns(newModel)
                    val sorter: TableRowSorter<ListTableModel<ConfigEvalRow>> = TableRowSorter(newModel)
                    for (colIdx in 0 until newModel.columnCount) {
                        val colName = newModel.getColumnName(colIdx)
                        if (colName.endsWith(" Score")) {
                            sorter.setComparator(colIdx) { a, b ->
                                fun parse(x: Any?): Double? =
                                    when (x) {
                                        null -> null
                                        is Number -> x.toDouble()
                                        is String -> x.trim().toDoubleOrNull()
                                        else -> x.toString().toDoubleOrNull()
                                    }

                                val da = parse(a)
                                val db = parse(b)
                                when {
                                    da != null && db != null -> da.compareTo(db)
                                    da != null -> 1
                                    db != null -> -1
                                    else -> a.toString().compareTo(b.toString(), ignoreCase = true)
                                }
                            }
                        }
                    }
                    table.rowSorter = sorter
                    if (scroll != null) {
                        enableEvalResultsMultiline(table, scroll)
                        adjustEvalResultColumnWidths(table)
                        adjustTableAutoResizeMode(table, scroll)
                    }
                    enableEvalResultsTopLeftAlignment(table)
                } catch (e: Exception) {
                    LOG.warn("Failed to rebuild table model", e)
                }
            }

            val count = maxOf(
                evalVM.tableIds.value.size,
                evalVM.tableRows.value.size,
                evalVM.tableOutputs.value.size
            )
            val newItems = (0 until count).map { idx ->
                val row =
                    evalVM.tableRows.value.getOrElse(idx) { TableRow("", emptyMap(), emptyMap()) }
                val output = evalVM.tableOutputs.value.getOrElse(idx) { "" }
                val id = (idx + 1).toString()
                ConfigEvalRow(id, row.input, output, row.evaluatorScores, row.evaluatorExtras)
            }
            activeModel.setItems(newItems)
            activeModel.fireTableDataChanged()
            (table.rowSorter as? TableRowSorter<*>)?.allRowsChanged()
            try {
                adjustEvalResultColumnWidths(table)
            } catch (e: Throwable) {
                LOG.warn("Failed to adjust column widths", e)
            }
            if (scroll != null) {
                try {
                    adjustTableAutoResizeMode(table, scroll)
                } catch (e: Throwable) {
                    LOG.warn("Failed to adjust table resize mode", e)
                }
            }
            try {
                val cols = getTextColumnIndices(table)
                updateVisibleRowHeights(table, cols)
            } catch (e: Throwable) {
                LOG.warn("Failed to update row heights", e)
            }
            table.revalidate()
            table.repaint()
            table.parent?.revalidate()
            table.parent?.repaint()

            if (scroll != null && evalVM.isRunning.value && count > 0) {
                scrollTimer?.stop()
                scrollTimer = Timer(300) {
                    try {
                        val lastRow = table.rowCount - 1
                        if (lastRow >= 0) {
                            val rect = table.getCellRect(lastRow, 0, true)
                            table.scrollRectToVisible(rect)
                        }
                    } catch (e: Throwable) {
                        LOG.warn("Failed to scroll to last row", e)
                    }
                }.apply {
                    isRepeats = false
                    start()
                }
            }
        }
        if (app.isDispatchThread) runnable.run() else app.invokeLater(runnable)
    }

    fun dispose() {
        scope.cancel()
    }

    private fun adjustColumnWidths(table: JTable) {
        val fm = table.getFontMetrics(table.tableHeader.font)
        for (column in 0 until table.columnCount) {
            val tableColumn: TableColumn = table.columnModel.getColumn(column)
            var preferredWidth = tableColumn.minWidth
            val maxWidth = 300

            val headerValue = tableColumn.headerValue?.toString() ?: ""
            val words = headerValue.trim().split(Regex("\\s+"))

            var minTwoLineWidth = fm.stringWidth(headerValue)

            if (words.size > 1) {
                var bestSplitWidth = Int.MAX_VALUE
                for (i in 1 until words.size) {
                    val line1 = words.subList(0, i).joinToString(" ")
                    val line2 = words.subList(i, words.size).joinToString(" ")
                    val currentWidth = maxOf(fm.stringWidth(line1), fm.stringWidth(line2))
                    if (currentWidth < bestSplitWidth) {
                        bestSplitWidth = currentWidth
                    }
                }
                minTwoLineWidth = bestSplitWidth
            }

            val targetMinWidth = minTwoLineWidth + 24
            tableColumn.minWidth = maxOf(tableColumn.minWidth, targetMinWidth)
            preferredWidth = maxOf(preferredWidth, targetMinWidth)

            val headerRenderer = table.tableHeader.defaultRenderer
            val headerComponent = headerRenderer.getTableCellRendererComponent(
                table, tableColumn.headerValue, false, false, 0, column
            )
            preferredWidth = maxOf(preferredWidth, headerComponent.preferredSize.width)

            for (row in 0 until table.rowCount) {
                val cellRenderer = table.getCellRenderer(row, column)
                val component = table.prepareRenderer(cellRenderer, row, column)
                preferredWidth = maxOf(preferredWidth, component.preferredSize.width)
            }

            preferredWidth += table.intercellSpacing.width
            tableColumn.preferredWidth = minOf(preferredWidth + 4, maxWidth) // Add padding
        }
    }
}