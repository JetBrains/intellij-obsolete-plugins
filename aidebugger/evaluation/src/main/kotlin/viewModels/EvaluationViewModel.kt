package com.intellij.aidebugger.evaluation.viewModels

import com.intellij.aidebugger.common.models.TracesDatasetsRepository
import com.intellij.aidebugger.evaluation.EvaluationBundle
import com.intellij.aidebugger.evaluation.models.entities.ConfigInfo
import com.intellij.aidebugger.evaluation.models.entities.ConfigRow
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.EvalRunConfig
import com.intellij.aidebugger.evaluation.models.remote.RemoteRunStateService
import com.intellij.aidebugger.evaluation.models.repositories.EvalConfigsRepository
import com.intellij.aidebugger.evaluation.models.repositories.EvaluationResultsRepository
import com.intellij.aidebugger.evaluation.models.repositories.TableRow
import com.intellij.aidebugger.evaluation.models.repositories.TableSnapshot
import com.intellij.aidebugger.evaluation.services.EvaluationPersistenceService
import com.intellij.aidebugger.evaluation.services.LocalEvaluationService
import com.intellij.aidebugger.evaluation.services.RemoteEvaluationOrchestrator
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class RunningAction { NONE, TRACER, EVAL, REMOTE }

const val CONFIG_SELECTED_KEY: String = "com.intellij.aidebugger.evaluationView.selectedConfig.v1"

class EvaluationViewModel(
    private val project: Project,
    private val resultsRepository: EvaluationResultsRepository,
    private val tracesDatasetsRepository: TracesDatasetsRepository,
    private val configsRepository: EvalConfigsRepository,
    private val localEvalService: LocalEvaluationService = project.service(),
    private val remoteEvalService: RemoteEvaluationOrchestrator = project.service(),
    private val persistenceService: EvaluationPersistenceService = project.service()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _statusText = MutableStateFlow("Idle")
    val statusText: StateFlow<String> = _statusText

    private val _runningAction = MutableStateFlow(RunningAction.NONE)
    val runningAction: StateFlow<RunningAction> = _runningAction

    private val _tableIds = MutableStateFlow(listOf<String>())
    val tableIds: StateFlow<List<String>> = _tableIds

    private val _tableRows = MutableStateFlow(listOf<TableRow>())
    val tableRows: StateFlow<List<TableRow>> = _tableRows

    private val _tableOutputs = MutableStateFlow(listOf<String>())
    val tableOutputs: StateFlow<List<String>> = _tableOutputs

    private val _configRows = MutableStateFlow<List<ConfigRow>>(emptyList())
    val configRows: StateFlow<List<ConfigRow>> = _configRows

    private val localListener = object : LocalEvaluationService.Listener {
        override fun onStatusChange(status: String) {
            _statusText.value = status
        }

        override fun onTableSnapshotUpdate(snapshot: TableSnapshot) {
            updateTable(snapshot)
        }

        override fun onError(title: String, message: String) {
            showError(title, message)
        }

        override fun onWarning(message: String) {
            showWarning(message)
        }
    }

    private val remoteListener = object : RemoteEvaluationOrchestrator.Listener {
        override fun onStatusChange(status: String) {
            _statusText.value = status
        }

        override fun onTableSnapshotUpdate(snapshot: TableSnapshot) {
            updateTable(snapshot)
        }

        override fun onError(@NlsContexts.DialogTitle title: String, @NlsContexts.DialogMessage message: String) {
            showError(title, message)
        }

        override fun onWarning(@NlsContexts.DialogTitle title: String, @NlsContexts.DialogMessage message: String) {
            showWarning(message)
        }

        override fun onConfirmationRequired(@NlsContexts.DialogTitle title: String, @NlsContexts.DialogMessage message: String): Boolean {
            // Blocking dialog call on EDT
            var result = false
            ApplicationManager.getApplication().invokeAndWait {
                val exitCode = Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon())
                result = (exitCode == Messages.YES)
            }
            return result
        }
    }

    init {
        scope.launch {
            tracesDatasetsRepository.datasetContentChanged.collect { changedDatasetName ->
                if (changedDatasetName != null) {
                    refreshConfigList()
                }
            }
        }
    }

    fun runEvaluation() {
        if (_isRunning.value) {
            stopEvaluation()
            return
        }

        val selectedName = getSelectedConfigName()
        if (selectedName == null) {
            showWarning(EvaluationBundle.message("eval.view.warning.no.config.selected"))
            return
        }
        persistenceService.invalidateCache(selectedName)

        _isRunning.value = true
        _runningAction.value = RunningAction.EVAL

        scope.launch {
            localEvalService.startEvaluation(selectedName, localListener)
            _isRunning.value = false
            _runningAction.value = RunningAction.NONE
            refreshConfigList()
        }
    }

    fun runRemoteEvaluation() {
        if (_isRunning.value) {
            if (_runningAction.value == RunningAction.REMOTE) {
                remoteEvalService.stopRemoteEvaluation(remoteListener)
                _isRunning.value = false
                _runningAction.value = RunningAction.NONE
            } else {
                stopEvaluation()
            }
            return
        }

        val selectedName = getSelectedConfigName()
        if (selectedName.isNullOrBlank()) {
            showWarning(EvaluationBundle.message("eval.view.warning.no.config.selected"))
            return
        }
        persistenceService.invalidateCache(selectedName)

        _isRunning.value = true
        _runningAction.value = RunningAction.REMOTE

        val wrappingListener = object : RemoteEvaluationOrchestrator.Listener by remoteListener {
            override fun onStatusChange(status: String) {
                remoteListener.onStatusChange(status)
                if (status.contains("Finished", true) || status.contains("Canceled", true) || status.contains("Failed", true) || status.contains("Aborted", true)) {
                    _isRunning.value = false
                    _runningAction.value = RunningAction.NONE
                    RemoteRunStateService.getInstance(project).clear()
                    refreshConfigList()
                }
            }
        }

        remoteEvalService.startRemoteEvaluation(selectedName, wrappingListener)
    }

    fun resumeRemotePollingIfNeeded() {
        val stateService = project.service<RemoteRunStateService>()
        val executionId = stateService.state.executionId ?: return
        val configName = stateService.state.configName ?: return

        setSelectedConfigName(configName)
        persistenceService.invalidateCache(configName)

        _isRunning.value = true
        _runningAction.value = RunningAction.REMOTE

        val wrappingListener = object : RemoteEvaluationOrchestrator.Listener by remoteListener {
            override fun onStatusChange(status: String) {
                remoteListener.onStatusChange(status)
                if (status.contains("Finished", true) || status.contains("Canceled", true) || status.contains("Failed", true)) {
                    _isRunning.value = false
                    _runningAction.value = RunningAction.NONE
                    RemoteRunStateService.getInstance(project).clear()
                    refreshConfigList()
                }
            }
        }

        remoteEvalService.resumeRemotePollingIfNeeded(executionId, configName, wrappingListener)
    }

    fun stopEvaluation() {
        val action = _runningAction.value
        if (action == RunningAction.EVAL || action == RunningAction.TRACER) {
            localEvalService.stopEvaluation(localListener, getSelectedConfigName())
        } else if (action == RunningAction.REMOTE) {
            remoteEvalService.stopRemoteEvaluation(remoteListener)
        }
        _isRunning.value = false
        _runningAction.value = RunningAction.NONE
    }

    fun getSelectedConfigName(): String? {
        return PropertiesComponent.getInstance(project).getValue(CONFIG_SELECTED_KEY)?.takeIf { it.isNotBlank() }
    }

    fun setSelectedConfigName(configName: String) {
        PropertiesComponent.getInstance(project).setValue(CONFIG_SELECTED_KEY, configName)
    }

    fun listConfigs(): List<ConfigInfo> = configsRepository.listConfigs()

    fun deleteConfig(name: String) {
        val info = configsRepository.listConfigs().firstOrNull { it.name == name }
        if (info != null) {
            configsRepository.removeConfig(info.name)
            resultsRepository.removeResults(info.name)
        }
    }

    fun duplicateConfig(name: String): ConfigInfo? {
        val info = configsRepository.listConfigs().firstOrNull { it.name == name } ?: return null
        val cfg = runCatching { configsRepository.loadConfig(info) }.getOrElse { EvalRunConfig() }
        val baseName = "${info.name}_copy"
        return configsRepository.createOrUpdateConfig(baseName, cfg)
    }

    fun getConfigIndexByName(configName: String): Int {
        return _configRows.value.indexOfFirst { it.name == configName }
    }

    fun showLastRunForConfig(configName: String) {
        if (_isRunning.value) return

        val snapshot = persistenceService.loadTableSnapshot(configName)
        updateTable(snapshot)

        scope.launch {
            persistenceService.getOrLoadDataPoints(configName)
        }
    }

    fun getOrLoadDataPoints(configName: String): List<DataPoint>? {
        return persistenceService.getOrLoadDataPoints(configName)
    }

    fun refreshConfigList() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val raw = configsRepository.listConfigs()
            val msMap: Map<String, Long> = raw.associate {
                it.name to (resultsRepository.getLastRunTimestamp(it.name) ?: Long.MIN_VALUE)
            }
            val sorted = raw.sortedByDescending { msMap[it.name] ?: Long.MIN_VALUE }

            val rows = sorted.map { info ->
                val cfg = runCatching { configsRepository.loadConfig(info) }.getOrElse { EvalRunConfig() }
                val dsRef = cfg.datasetName

                val cnt = tracesDatasetsRepository.countRowsForDataset(dsRef)
                val label = tracesDatasetsRepository.resolveDataset(dsRef)?.name ?: ""

                val ms = msMap[info.name]?.takeIf { it > 0 }
                val avgTimeStr = formatAvgAgentTime(resultsRepository.getAverageAgentTime(info.name))
                val avgTokStr = formatAvgTokens(resultsRepository.getAverageTokens(info.name))
                val dateStr = formatTimestamp(ms)

                val agg = resultsRepository.getLastAggregatedResult(info.name)
                val scores = agg?.evaluatorsStats?.mapValues { entry ->
                    DecimalFormat("#.##").format(entry.value.mean)
                } ?: emptyMap()

                ConfigRow(
                    info.name,
                    label,
                    cnt.toString(),
                    avgTimeStr,
                    avgTokStr,
                    dateStr,
                    scores
                )
            }
            _configRows.value = rows
        }
    }

    private fun updateTable(snapshot: TableSnapshot) {
        _tableIds.value = snapshot.ids
        _tableRows.value = snapshot.rows
        _tableOutputs.value = snapshot.outputs
    }

    private fun showWarning(@NlsContexts.DialogMessage message: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showWarningDialog(project, message, EvaluationBundle.message("eval.warning.dialog.title"))
        }
    }

    private fun showError(@NlsContexts.DialogTitle title: String, @NlsContexts.DialogMessage message: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(project, message, title)
        }
    }

    private fun formatAvgAgentTime(avgSec: Double): String {
        return if (avgSec.isNaN()) "" else DecimalFormat("#.##").format(avgSec)
    }

    private fun formatAvgTokens(avgTokens: Int): String {
        return if (avgTokens == 0) "" else avgTokens.toString()
    }

    private fun formatTimestamp(ms: Long?): String {
        if (ms == null) return ""
        return try {
            val zdt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            zdt.format(fmt)
        } catch (_: Throwable) { "" }
    }
}
