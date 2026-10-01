package com.intellij.aidebugger.evaluation.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.aidebugger.common.models.DatasetInfo
import com.intellij.aidebugger.common.models.TracesDatasetsRepository
import com.intellij.aidebugger.evaluation.EvaluationBundle
import com.intellij.aidebugger.evaluation.intellij.RunConfigTargetResolver
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.remote.RemoteRunStateService
import com.intellij.aidebugger.evaluation.models.repositories.EvalConfigsRepositoryImpl
import com.intellij.aidebugger.evaluation.models.repositories.TableRow
import com.intellij.aidebugger.evaluation.models.repositories.TableSnapshot
import com.intellij.aidebugger.evaluation.models.storage.JsonFileStorage.deleteRecursively
import com.intellij.execution.RunManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.python.run.PythonRunConfiguration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Service(Service.Level.PROJECT)
class RemoteEvaluationOrchestrator(private val project: Project) {
    companion object {
        const val DOCKER_IMAGE = "registry.jetbrains.team/p/mlops/ai-toolkit/eval:latest"
        const val DOCKER_IMAGE_PREFIX = "registry.jetbrains.team/p/mlops/ai-toolkit"
    }
    private val LOG = Logger.getInstance(RemoteEvaluationOrchestrator::class.java)

    private val remoteExecutionService = project.service<RemoteExecutionService>()
    private val configsRepository = project.service<EvalConfigsRepositoryImpl>()
    private val tracesDatasetsRepository = project.service<TracesDatasetsRepository>()
    private val installer = project.service<PluginDownloaderService>()
    private val persistenceService = project.service<EvaluationPersistenceService>()
    private val pythonDetector = PythonDependencyManagerDetector(project)

    private val _currentExecutionId = MutableStateFlow<String?>(null)

    private var remoteResultsJob: Job? = null
    private var pluginInstallJob: Job? = null

    private var currentIds: List<String> = emptyList()
    private var currentRows: MutableList<TableRow> = mutableListOf()
    private var currentOutputs: MutableList<String> = mutableListOf()

    private val lastRunDataPointsByKey: MutableMap<String, List<DataPoint>> = mutableMapOf()

    interface Listener {
        fun onStatusChange(status: String)
        fun onTableSnapshotUpdate(snapshot: TableSnapshot)
        fun onError(@NlsContexts.DialogTitle title: String, @NlsContexts.DialogMessage message: String)
        fun onWarning(@NlsContexts.DialogTitle title: String, @NlsContexts.DialogMessage message: String)
        fun onConfirmationRequired(@NlsContexts.DialogTitle title: String, @NlsContexts.DialogMessage message: String): Boolean
    }

    /**
     * Starts the remote evaluation flow.
     */
    fun startRemoteEvaluation(configName: String, listener: Listener) {
        val info = configsRepository.listConfigs().firstOrNull { it.name == configName }
        if (info == null) {
            listener.onWarning(EvaluationBundle.message("eval.remote.eval.title"), EvaluationBundle.message("eval.remote.eval.warning.config.not.found", configName))
            return
        }

        val runCfg = runCatching { configsRepository.loadConfig(info) }.getOrNull()
        if (runCfg == null) {
            listener.onWarning(EvaluationBundle.message("eval.remote.eval.title"), EvaluationBundle.message("eval.remote.eval.warning.config.load.failed", configName))
            return
        }

        val datasetName = runCfg.datasetName?.trim().orEmpty()
        if (datasetName.isBlank()) {
            listener.onWarning(EvaluationBundle.message("eval.remote.eval.title"), EvaluationBundle.message("eval.remote.eval.warning.no.dataset"))
            return
        }

        val base = project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
        val fileName = if (datasetName.lowercase().endsWith(".json")) datasetName else "$datasetName.json"
        val datasetPath = base.resolve(".jbeval").resolve("datasets").resolve(fileName)
        if (!Files.exists(datasetPath) || !Files.isRegularFile(datasetPath)) {
            listener.onError(EvaluationBundle.message("eval.remote.eval.title"), EvaluationBundle.message("eval.remote.eval.error.dataset.not.found", datasetPath))
            return
        }

        if (!installer.isCadencePluginInstalled()) {
            if (listener.onConfirmationRequired(EvaluationBundle.message("eval.cadence.plugin.required.title"), EvaluationBundle.message("eval.cadence.plugin.required.message"))) {
                // proceed
            } else {
                return
            }
        }

        remoteResultsJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                performRemoteExecution(configName, datasetName, runCfg.runConfigName, listener)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                LOG.warn("Failed to start remote evaluation: ${t.message}", t)
                listener.onStatusChange("Remote eval: Failed")
            }
        }
    }

    private suspend fun performRemoteExecution(
        configName: String,
        datasetName: String,
        runConfigName: String?,
        listener: Listener
    ) {
        listener.onStatusChange("Remote eval: starting…")

        seedPlaceholdersFromActiveDataset(configName, listener)

        val targetFlags = RunConfigTargetResolver.buildTargetFlagsFromRunConfig(project, runConfigName)
        if (targetFlags == null) {
            listener.onError(EvaluationBundle.message("eval.remote.eval.title"), EvaluationBundle.message("eval.remote.eval.error.no.script"))
            listener.onStatusChange("Remote eval: Aborted")
            return
        }

        createRemoteNextSubdir(configName, datasetName)
        copyScriptToRemoteNext(configName, targetFlags)

        val rm = RunManager.getInstance(project)
        val settings = if (!runConfigName.isNullOrBlank())
            rm.allSettings.firstOrNull { it.name == runConfigName }
        else
            rm.selectedConfiguration

        val actualConfigName = settings?.configuration?.name
        if (actualConfigName == null) {
            listener.onStatusChange("Remote eval: Aborted (no run configuration)")
            return
        }

        val envFileFlag = run {
            val projectPath = project.basePath?.let { Path.of(it) }
            val envPath = projectPath?.resolve(".env")
            if (envPath != null && Files.exists(envPath)) "--env-file ./.env" else ""
        }

        val pythonVersion = run {
            val cfg = settings.configuration
            if (cfg is PythonRunConfiguration) pythonDetector.resolvePythonVersion(cfg) else null
        }

        val conf: PythonDependencyManagerDetector.PythonEnvConf? = run {
            val projectRootPath = project.guessProjectDir()?.toNioPath()
            if (projectRootPath == null) null else {
                val isModuleMode = targetFlags.trim().startsWith("--module")
                if (isModuleMode) {
                    pythonDetector.findPythonDependencyManager()
                } else {
                    val regex = Regex("--script\\s+'([^']+)'")
                    val match = regex.find(targetFlags)
                    if (match != null) {
                        val workspacePath = match.groupValues.getOrNull(1)
                        val rel = if (!workspacePath.isNullOrBlank()) {
                            if (workspacePath.startsWith("/workspace/")) workspacePath.removePrefix("/workspace/") else workspacePath.trimStart('/')
                        } else null
                        if (!rel.isNullOrBlank()) {
                            val abs = projectRootPath.resolve(rel).normalize()
                            val maxDir = abs.parent ?: projectRootPath
                            pythonDetector.findPythonDependencyManager(projectRootPath, maxDir)
                        } else null
                    } else null
                }
            }
        }

        val (tool, path) = pythonDetector.toShellVars(conf)
        val pipConf = tool == "pip"
        val poetryConf = tool == "poetry"
        val envConfigPath = "/workspace/${path.trimStart('/')}"

        val options = buildString {
            append("--rm ")
            append(envFileFlag).append(' ')
            append("--env PYTHON_ENV_TOOL=$tool ")
            append("--env PYTHON_ENV_CONFIG_PATH=$envConfigPath ")
            pythonVersion?.let { append("--env PYTHON_VERSION=$it ") }
            append("-v \"\$(pwd)\":/workspace -w /workspace")
        }

        val dockerImage = dockerImage(pythonVersion)

        val command = "sudo docker run $options $dockerImage"

        val defaultName = "${project.name} evaluation"

        val requestBody = RemoteExecutionRequest(
            name = defaultName,
            root = ".",
            workingDir = ".",
            command = command,
            dockerImage = null,
            dockerAdditionalArgs = null,
            includesPath = emptyList(),
            outputPath = ".",
            pythonVersion = pythonVersion,
            pipRequirementsPath = path.takeIf { pipConf },
            poetryDirPath = path.takeIf { poetryConf },
            variables = null,
            secretVariables = null,
            runConfigName = actualConfigName,
            provisioningConf = ProvisioningConf(null, 0, 4, 16),
        )

        val (executionId, pollingJob) = remoteExecutionService.startAndPoll(
            request = requestBody,
            pollIntervalMs = 4000L,
            onStatusUpdate = { status ->
                updateRemoteOutputsStatus(status, listener)
            },
            onTerminal = {
                val currentId = _currentExecutionId.value
                if (currentId != null) {
                    handleTerminalState(currentId, configName, listener)
                } else {
                    listener.onStatusChange("Remote eval: Completed")
                }
            }
        )

        _currentExecutionId.value = executionId
        pluginInstallJob = pollingJob
    }

    private fun dockerImage(version: String?): String {
        return when {
            version?.startsWith("3.10") == true -> "$DOCKER_IMAGE_PREFIX/eval-py-3.10:latest"
            version?.startsWith("3.11") == true -> "$DOCKER_IMAGE_PREFIX/eval-py-3.11:latest"
            version?.startsWith("3.12") == true -> "$DOCKER_IMAGE_PREFIX/eval-py-3.12:latest"
            version?.startsWith("3.13") == true -> "$DOCKER_IMAGE_PREFIX/eval-py-3.13:latest"
            version?.startsWith("3.14") == true -> "$DOCKER_IMAGE_PREFIX/eval-py-3.14:latest"
            else -> DOCKER_IMAGE
        }
    }

    fun resumeRemotePollingIfNeeded(executionId: String, configName: String, listener: Listener) {
        _currentExecutionId.value = executionId

        if (!installer.isCadencePluginInstalled()) {
            if (listener.onConfirmationRequired(EvaluationBundle.message("eval.cadence.plugin.required.title"), EvaluationBundle.message("eval.cadence.plugin.required.message"))) {
                // proceed
            } else {
                return
            }
        }

        seedPlaceholdersFromActiveDataset(configName, listener)

        remoteExecutionService.pollStatus(
            executionId = executionId,
            pollIntervalMs = 4000L,
            onStatusUpdate = { status ->
                updateRemoteOutputsStatus(status, listener)
            },
            onTerminal = {
                val currentId = _currentExecutionId.value
                if (currentId != null) {
                    handleTerminalState(currentId, configName, listener)
                    // todo call process results
                } else {
                    listener.onStatusChange("Remote eval: Completed")
                }
            }
        )
    }

    private fun seedPlaceholdersFromActiveDataset(configName: String, listener: Listener) {
        val info = configsRepository.listConfigs().firstOrNull { it.name == configName }!!

        val runCfg = runCatching { configsRepository.loadConfig(info) }.getOrNull()
        if (runCfg == null) {
            listener.onWarning(EvaluationBundle.message("eval.remote.eval.title"), EvaluationBundle.message("eval.remote.eval.warning.config.load.failed", configName))
            return
        }

        val datasetName = runCfg.datasetName?.trim().orEmpty()
        if (datasetName.isBlank()) {
            listener.onWarning(EvaluationBundle.message("eval.remote.eval.title"), EvaluationBundle.message("eval.remote.eval.warning.no.dataset"))
            return
        }

        val base = project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
        val dsFileName = if (datasetName.lowercase().endsWith(".json")) datasetName else "$datasetName.json"
        val datasetPath = base.resolve(".jbeval").resolve("datasets").resolve(dsFileName)

        val loadedInputs = loadInputsFromDataset(datasetPath, datasetName)
        if (!loadedInputs.isNullOrEmpty()) {
            currentIds = loadedInputs.map { "dp_cfg_${System.currentTimeMillis()}_${(0..999999).random()}" }
            currentRows = loadedInputs.map { TableRow(it, emptyMap(), emptyMap()) }.toMutableList()
            currentOutputs = loadedInputs.map { "Remote evaluation pending…" }.toMutableList()
            notifySnapshot(listener)
        } else {
            val placeholderId = "dp_cfg_${System.currentTimeMillis()}_${(0..999999).random()}"
            currentIds = listOf(placeholderId)
            currentRows = mutableListOf(TableRow("Dataset is empty or could not be loaded…", emptyMap(), emptyMap()))
            currentOutputs = mutableListOf("Remote evaluation pending…")
            notifySnapshot(listener)
        }
    }

    private fun handleTerminalState(executionId: String, configName: String, listener: Listener) {
        val remotePath = project.basePath?.let { Path.of(it) }?.resolve("remote")
        if (remotePath != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val resolvedPath = remoteExecutionService.downloadOutputs(executionId, remotePath, configName)

                    moveRemoteNextSubdirToRemote(configName)
                    processRemoteResults(configName, resolvedPath, listener)

                    listener.onStatusChange("Remote eval: Completed")
                } catch (t: Throwable) {
                    LOG.warn("Failed to process remote results", t)
                    listener.onStatusChange("Remote eval: Completed (with errors)")
                } finally {
                    _currentExecutionId.value = null
                }
            }
        } else {
            listener.onStatusChange("Remote eval: Completed")
            _currentExecutionId.value = null
        }
    }

    fun ensurePollingCanceledForCurrentRun() {
        val id = _currentExecutionId.value
        if (id.isNullOrBlank()) return
        try {
            runCatching { remoteExecutionService.cancelPolling(id) }
        } catch (_: Throwable) { }
        _currentExecutionId.value = null
        runCatching { RemoteRunStateService.getInstance(project).clear() }
    }

    fun stopRemoteEvaluation(listener: Listener) {
        val id = _currentExecutionId.value

        try { remoteResultsJob?.cancel() } catch (_: Throwable) { }
        remoteResultsJob = null
        try { pluginInstallJob?.cancel() } catch (_: Throwable) { }

        if (pluginInstallJob?.isActive == true) {
            if (listener.onConfirmationRequired(EvaluationBundle.message("eval.cadence.cancel.title"), EvaluationBundle.message("eval.cadence.cancel.message"))) {
                pluginInstallJob?.cancel()
                installer.cancelPluginInstallation()
                listener.onStatusChange("Plugin installation canceled")
            }
            return
        }
        pluginInstallJob = null

        if (id.isNullOrBlank()) {
            listener.onStatusChange("Remote eval: Canceled")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            RemoteRunStateService.getInstance(project).clear()
            try {
                updateRemoteOutputsStatus("Canceling...", listener)
                remoteExecutionService.stop(id)
                runCatching { remoteExecutionService.cancelPolling(id) }
                _currentExecutionId.value = null
                updateRemoteOutputsStatus("canceled", listener)
                listener.onStatusChange("Remote eval: Canceled")
            } catch (t: Throwable) {
                LOG.warn("Failed to stop remote execution", t)
                updateRemoteOutputsStatus("Failed to stop: ${t.message}", listener)
                listener.onStatusChange("Remote eval: Stop failed")
            }
        }

        ensurePollingCanceledForCurrentRun()
    }

    private fun updateRemoteOutputsStatus(status: String, listener: Listener) {
        listener.onStatusChange("Remote: $status")
        for (i in currentOutputs.indices) {
            currentOutputs[i] = status
        }
        notifySnapshot(listener)
    }

    private fun notifySnapshot(listener: Listener) {
        listener.onTableSnapshotUpdate(TableSnapshot(currentIds, currentRows.toList(), currentOutputs.toList()))
    }

    private fun processRemoteResults(configName: String, targetPath: Path, listener: Listener) {
        val info = configsRepository.listConfigs().firstOrNull { it.name == configName }
        val runCfg = info?.let { configsRepository.loadConfig(it) }
        val runConfigName = runCfg?.runConfigName

        val result = remoteExecutionService.parseResults(targetPath, runConfigName)

        val idToIndexMap = currentIds.withIndex().associate { (index, id) -> id to index }
        var matchedCount = 0

        for ((inp, pair) in result.resultsByInput) {
            val idx = findRowIndexById(inp, idToIndexMap) ?: findRowIndexByOrder(matchedCount, currentRows.size)
            if (idx == null) continue

            matchedCount++
            val (scoresMap, extrasMap) = pair
            val currentRow = currentRows[idx]
            currentRows[idx] = TableRow(currentRow.input, scoresMap, extrasMap)
        }

        var outputMatchCount = 0
        for ((inp, outText) in result.outputsByInput) {
            val idx = findRowIndexById(inp, idToIndexMap) ?: findRowIndexByOrder(outputMatchCount, currentOutputs.size)
            if (idx != null && idx in currentOutputs.indices) {
                currentOutputs[idx] = outText
                outputMatchCount++
            }
        }

        val snapshot = TableSnapshot(currentIds, currentRows.toList(), currentOutputs.toList())
        notifySnapshot(listener)
        persistenceService.saveTableSnapshot(configName, snapshot)

        result.meanScore?.let { listener.onStatusChange("Remote: results loaded (mean=$it)") }
    }

    private fun findRowIndexById(input: String, idToIndexMap: Map<String, Int>): Int? {
        val idPattern = Regex("dp_cfg_\\d+_\\d+")
        val match = idPattern.find(input)
        return match?.value?.let { idToIndexMap[it] }
    }

    private fun findRowIndexByOrder(currentMatchCount: Int, totalRows: Int): Int? {
        return if (currentMatchCount < totalRows) currentMatchCount else null
    }

    private fun loadInputsFromDataset(datasetPath: Path, datasetName: String): List<String>? {
        loadInputsFromTracesDataset(datasetPath)?.let { return it }
        loadInputsFromDataPointsJson(datasetPath)?.let { return it }
        loadInputsFromDatasetJson(datasetPath)?.let { return it }
        return loadInputsFromCache(datasetName)
    }

    private fun loadInputsFromTracesDataset(datasetPath: Path): List<String>? {
        return try {
            val all = tracesDatasetsRepository.listDatasets()
            val info = all.firstOrNull {
                val base = project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
                base.resolve(".jbeval").resolve("datasets").resolve(it.fileName) == datasetPath
            } ?: return null

            val rows = tracesDatasetsRepository.loadDataset(info, null, null)
            val inputs = rows.mapNotNull { row -> row.input?.trim() }.filter { it.isNotEmpty() }
            inputs.ifEmpty { null }
        } catch (_: Throwable) { null }
    }

    private fun loadInputsFromDataPointsJson(datasetPath: Path): List<String>? {
        return try {
            val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            val type = mapper.typeFactory.constructCollectionType(java.util.List::class.java, DataPoint::class.java)
            Files.newBufferedReader(datasetPath).use { br ->
                val dps: List<DataPoint> = mapper.readValue(br, type)
                val inputs = dps.mapNotNull { dp -> (dp.input ?: dp.id ?: "").trim().ifEmpty { null } }
                inputs.ifEmpty { null }
            }
        } catch (_: Throwable) { null }
    }

    private fun loadInputsFromDatasetJson(datasetPath: Path): List<String>? {
        return try {
            val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            Files.newBufferedReader(datasetPath).use { br ->
                val root = mapper.readTree(br)
                val items = root?.get("items")
                if (items != null && items.isArray) {
                    val inputs = items.mapNotNull { node ->
                        val inpNode = node.get("input") ?: node.get("id")
                        when {
                            inpNode == null || inpNode.isNull -> null
                            inpNode.isTextual -> inpNode.asText().trim().ifEmpty { null }
                            else -> inpNode.toString().trim().ifEmpty { null }
                        }
                    }
                    inputs.ifEmpty { null }
                } else null
            }
        } catch (_: Throwable) { null }
    }

    private fun loadInputsFromCache(datasetName: String): List<String>? {
        val key = datasetName.trim()
        if (key.isEmpty()) return null
        return lastRunDataPointsByKey[key]?.map { (it.input ?: it.id ?: "").trim() }?.filter { it.isNotEmpty() }
    }

    private fun resolveTracesDatasetByPath(dsRef: String): DatasetInfo? {
        val base = runCatching {
            Paths.get(project.basePath ?: System.getProperty("user.dir"))
                .resolve(".jbeval").resolve("datasets")
        }.getOrNull() ?: return null

        val fileName = Path.of(dsRef).fileName.toString()
        return tracesDatasetsRepository.listDatasets().firstOrNull { d ->
            d.fileName == fileName || base.resolve(d.fileName).toString() == dsRef
        }
    }

    private fun createRemoteNextSubdir(selectedName: String?, datasetName: String?) {
        if (selectedName.isNullOrBlank()) return
        try {
            val projectPath = project.guessProjectDir()?.toNioPath() ?: return
            val remoteBase = projectPath.resolve(".jbeval").resolve("remote")
            val nextDir = remoteBase.resolve("next")

            runCatching { deleteRecursively(nextDir) }.onFailure { }
            Files.createDirectories(nextDir)

            val destDir = nextDir.resolve(selectedName)
            Files.createDirectories(destDir)

            val dsName = datasetName?.trim().orEmpty()
            if (dsName.isNotEmpty()) {
                val src = projectPath.resolve(".jbeval").resolve("datasets").resolve("$dsName.json")
                if (Files.exists(src)) {
                    val dst = destDir.resolve("dataset.json")
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING)
                }
            }

            val srcConfig = configsRepository.getConfigPathByName(selectedName)
            if (srcConfig != null && Files.exists(srcConfig)) {
                val configDst = destDir.resolve("$selectedName.json")
                Files.copy(srcConfig, configDst, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) {
            LOG.warn("Failed to create remote next subdir", e)
        }
    }

    private fun copyScriptToRemoteNext(selectedName: String?, targetFlags: String?) {
        if (selectedName.isNullOrBlank() || targetFlags.isNullOrBlank()) return
        try {
            val regex = Regex("--script\\s+'([^']+)'")
            val match = regex.find(targetFlags) ?: return
            val workspacePath = match.groupValues.getOrNull(1) ?: return
            val rel = if (workspacePath.startsWith("/workspace/")) workspacePath.removePrefix("/workspace/") else workspacePath.trimStart('/')

            val projectPath = project.guessProjectDir()?.toNioPath() ?: return
            val src = projectPath.resolve(rel).normalize()
            if (!Files.exists(src) || Files.isDirectory(src)) return

            val destDir = projectPath.resolve(".jbeval").resolve("remote").resolve("next").resolve(selectedName)
            Files.createDirectories(destDir)
            val dst = destDir.resolve("script.py")
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: Exception) { }
    }

    private fun moveRemoteNextSubdirToRemote(selectedName: String?) {
        if (selectedName.isNullOrBlank()) return
        try {
            val projectPath = project.guessProjectDir()?.toNioPath() ?: return
            val remoteBase = projectPath.resolve(".jbeval").resolve("remote")
            val nextDir = remoteBase.resolve("next")
            val src = nextDir.resolve(selectedName)
            val dst = remoteBase.resolve(selectedName)

            if (!Files.exists(src)) return
            Files.createDirectories(remoteBase)
            runCatching { deleteRecursively(dst) }.onFailure { }
            Files.move(src, dst)
        } catch (_: Exception) { }
    }
}