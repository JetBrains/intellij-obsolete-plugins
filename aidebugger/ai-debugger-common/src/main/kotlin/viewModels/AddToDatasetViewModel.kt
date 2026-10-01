package com.intellij.aidebugger.common.viewModels

import com.google.gson.Gson
import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.common.models.HierarchicalTraceEventsState
import com.intellij.aidebugger.common.models.JsonPathDefaults
import com.intellij.aidebugger.common.models.RunnerConfigRepository
import com.intellij.aidebugger.common.models.TracesDatasetsRepository
import com.intellij.aidebugger.common.models.serializeHierarchicalTraceEventsStateToMap
import com.intellij.aidebugger.common.services.GlobalSettingsService
import com.intellij.aidebugger.common.utility.JsonPathToken
import com.intellij.aidebugger.common.utility.JsonPathUtils
import com.intellij.aidebugger.common.views.components.showAddTracePopup
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path

class AddToDatasetViewModel(
    private val project: Project,
    private val datasetsRepo: TracesDatasetsRepository,
    val coroutineScope: CoroutineScope
) : ViewModelBase {
    private val _showPopup = MutableStateFlow(false)
    val showPopup: StateFlow<Boolean> = _showPopup

    private val _isCreatingNew = MutableStateFlow(false)
    val isCreatingNew: StateFlow<Boolean> = _isCreatingNew

    private val _newDatasetName = MutableStateFlow("")
    val newDatasetName: StateFlow<String> = _newDatasetName

    private val _selectedThreadId = MutableStateFlow<String?>(null)
    private val _hasAnyRunningThreads = MutableStateFlow(false)
    private val _hierarchicalState = MutableStateFlow<HierarchicalTraceEventsState?>(null)
    val hierarchicalState = _hierarchicalState.asStateFlow()

    val isVisible: StateFlow<Boolean> = GlobalSettingsService.getInstance().isEvaluationEnabled
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val datasets: StateFlow<List<String>> = datasetsRepo.datasets
        .map { list -> list.map { it.name } }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val addedHashesByThread = mutableMapOf<String, MutableMap<String, MutableSet<Int>>>()

    private val _currentCheckboxStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val currentCheckboxStates: StateFlow<Map<String, Boolean>> = _currentCheckboxStates

    val canAddToDataset: StateFlow<Boolean> = combine(
        _hasAnyRunningThreads,
        _selectedThreadId,
        _hierarchicalState
    ) { hasRunning, threadId, state ->
        val isLangGraph = state?.rootEvents?.find { it.id == threadId }?.name == "LangGraph"
        !hasRunning && threadId != null && isLangGraph
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    private var updateChecksJob: Job? = null

    init {
        coroutineScope.launch {
            isVisible.combine(hierarchicalState) { visible, state ->
                if (!visible) null
                else state?.rootEvents?.firstOrNull()
            }.filterNotNull().first().let { rootEvent ->
                AiDebuggerCollector.reportAddToDatasetShown(project, rootEvent.framework)
            }
        }

        coroutineScope.launch {
            combine(isVisible, canAddToDataset, hierarchicalState) {
                isVisible, canAddToDataset, hierarchicalState ->
                if (isVisible && canAddToDataset && hierarchicalState != null) hierarchicalState.rootEvents.firstOrNull()
                else null
            }.filterNotNull().first().let { rootEvent ->
                AiDebuggerCollector.reportAddToDatasetBecameClickable(project, rootEvent.framework)
            }
        }
    }

    fun setSelectedThread(thread: SessionThreadVM?, isRunning: Boolean, hstate: HierarchicalTraceEventsState?) {
        _selectedThreadId.value = thread?.threadId
        _hasAnyRunningThreads.value = isRunning
        _hierarchicalState.value = hstate

        updateCheckboxStates(thread?.threadId, hstate)

        if (isRunning) {
            _showPopup.value = false
        }
    }

    private fun updateCheckboxStates(threadId: String?, state: HierarchicalTraceEventsState?) {
        updateChecksJob?.cancel()

        if (threadId == null || state == null) {
            _currentCheckboxStates.value = emptyMap()
            return
        }

        updateChecksJob = coroutineScope.launch(Dispatchers.Default) {
            val gson = Gson()
            // We assume state is a snapshot and safe to access here
            val rawStateMap = serializeHierarchicalTraceEventsStateToMap(state)
            val rawJsonString = gson.toJson(rawStateMap)
            val currentHash = rawJsonString.hashCode()

            val newStates = synchronized(addedHashesByThread) {
                val threadMap = addedHashesByThread[threadId] ?: return@synchronized emptyMap<String, Boolean>()
                threadMap.mapValues { (_, hashes) -> hashes.contains(currentHash) }
            }
            _currentCheckboxStates.value = newStates
        }
    }

    fun togglePopup() {
        val newState = !_showPopup.value
        _showPopup.value = newState

        if (newState) {
            coroutineScope.launch {
                datasetsRepo.refresh()
            }
        }
    }

    fun closePopup() {
        _showPopup.value = false
    }

    fun startCreatingDataset() {
        AiDebuggerCollector.reportAddToDatasetPopupNewDatasetCreationStarted(project)
        _isCreatingNew.value = true
        _newDatasetName.value = ""
    }

    fun setNewDatasetName(name: String) {
        _newDatasetName.value = name
    }

    fun commitNewDataset() {
        val name = _newDatasetName.value.trim()
        if (name.isNotEmpty()) {
            coroutineScope.launch {
                val framework = hierarchicalState.value?.rootEvents?.firstOrNull()?.framework
                AiDebuggerCollector.reportCreatedNewDatasetFromPopup(project, framework)

                datasetsRepo.createDataset(name)

                val threadId = _selectedThreadId.value
                if (threadId != null) {
                    synchronized(addedHashesByThread) {
                        val checks = addedHashesByThread.getOrPut(threadId) { mutableMapOf() }
                        checks.getOrPut(name) { mutableSetOf() }
                    }
                    updateCheckboxStates(threadId, _hierarchicalState.value)
                }

                cancelNewDataset()
            }
        }
    }

    fun cancelNewDataset() {
        AiDebuggerCollector.reportAddToDatasetPopupNewDatasetCreationCancelled(project)
        _isCreatingNew.value = false
        _newDatasetName.value = ""
    }

    fun addToDataset(datasetName: String, onResult: (Boolean) -> Unit) {
        val state = _hierarchicalState.value
        if (state == null || state.rootEvents.isEmpty()) {
            onResult(false)
            return
        }

        coroutineScope.launch {
            val gson = Gson()
            val rawStateMap = serializeHierarchicalTraceEventsStateToMap(state)
            val rootJson = gson.toJsonTree(rawStateMap)
            val rawJsonString = gson.toJson(rawStateMap)

            val runConfigName = getCurrentRunConfigName()
            val (inPath, exPath) = resolveJsonPathsForRunConfig(runConfigName)
            val defaultInputValue = JsonPathUtils.extractAsString(rootJson, inPath) ?: ""

            val dialogResult = showAddTracePopup(
                project = project,
                datasetName = datasetName,
                defaultInputPath = inPath,
                defaultInputValue = defaultInputValue,
                defaultExpectedPath = exPath,
                rawJson = rawJsonString,
                hierarchicalState = hierarchicalState.value,
            )

            if (!dialogResult.confirmed) {
                onResult(false)
                return@launch
            }

            val confirmedInputStruct = try {
                extractInputStructure(dialogResult.inputPath, rootJson)
            } catch (e: IllegalStateException) {
                null
            }

            val added = withContext(Dispatchers.IO) {
                datasetsRepo.addEntry(
                    datasetName,
                    dialogResult.inputValue,
                    dialogResult.expectedValue,
                    dialogResult.rawJson,
                )
            }

            if (added) {
                val runConfigName = getCurrentRunConfigName() ?: datasetName
                val basePath = project.basePath?.let { Path.of(it) }
                    ?: Path.of(System.getProperty("user.dir"))

                withContext(Dispatchers.IO) {
                    RunnerConfigRepository.saveRunnerConfig(
                        basePath = basePath,
                        runConfigName = runConfigName,
                        inputPath = dialogResult.inputPath,
                        outputPath = dialogResult.expectedPath,
                        inputStruct = confirmedInputStruct
                    )
                }

                val threadId = _selectedThreadId.value
                if (threadId != null) {
                    // We use rawJsonString hash to identify the state, not the dialog result
                    val hash = rawJsonString.hashCode()
                    synchronized(addedHashesByThread) {
                        val checks = addedHashesByThread.getOrPut(threadId) { mutableMapOf() }
                        checks.getOrPut(datasetName) { mutableSetOf() }.add(hash)
                    }
                    updateCheckboxStates(threadId, state)
                }

                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    private fun getCurrentRunConfigName(): String? {
        return try {
            val runManager = com.intellij.execution.RunManager.getInstance(project)
            runManager.selectedConfiguration?.name
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveJsonPathsForRunConfig(runConfigName: String?): Pair<String, String> {
        if (runConfigName.isNullOrBlank()) {
            return JsonPathDefaults.DEFAULT_INPUT_PATH to JsonPathDefaults.DEFAULT_OUTPUT_PATH
        }

        val basePath = project.basePath?.let { Path.of(it) }
            ?: Path.of(System.getProperty("user.dir"))

        val runnerConfig = RunnerConfigRepository.loadRunnerConfig(basePath, runConfigName)

        val inputPath = runnerConfig?.mapping?.input?.trim()?.takeIf { it.isNotEmpty() }
            ?: JsonPathDefaults.DEFAULT_INPUT_PATH

        val outputPath = runnerConfig?.mapping?.output?.trim()?.takeIf { it.isNotEmpty() }
            ?: JsonPathDefaults.DEFAULT_OUTPUT_PATH

        return inputPath to outputPath
    }

    private fun extractInputStructure(path: String, rootJson: com.google.gson.JsonElement): Any? {
        if (path.isBlank()) return null

        val tokens = JsonPathUtils.parseJsonPath(path)

        val inputSegmentIdx = tokens.indexOfLast { tok ->
            tok is JsonPathToken.Segment && (tok.name == "input" || tok.name == "inputs")
        }

        if (inputSegmentIdx == -1 || inputSegmentIdx == tokens.size - 1) {
            return null
        }

        val tokensUpToInput = tokens.subList(0, inputSegmentIdx + 1)
        val inputElement = JsonPathUtils.getValueByPath(rootJson, tokensUpToInput)

        return when (inputElement) {
            is com.google.gson.JsonObject -> inputElement
            is com.google.gson.JsonArray -> inputElement
            else -> null
        }
    }
}