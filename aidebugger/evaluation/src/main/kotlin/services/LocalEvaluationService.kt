package com.intellij.aidebugger.evaluation.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.aidebugger.common.models.DatasetInfo
import com.intellij.aidebugger.common.models.RunnerConfigRepository
import com.intellij.aidebugger.common.models.TracesDatasetsRepository
import com.intellij.aidebugger.evaluation.EvaluationBundle
import com.intellij.aidebugger.evaluation.EvaluationCollector
import com.intellij.aidebugger.evaluation.EvaluationError
import com.intellij.aidebugger.evaluation.EvaluationException
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.EvalResult
import com.intellij.aidebugger.evaluation.models.entities.EvalRunConfig
import com.intellij.aidebugger.evaluation.models.entities.InputSpec
import com.intellij.aidebugger.evaluation.models.entities.LLMScore
import com.intellij.aidebugger.evaluation.models.entities.extractExtraText
import com.intellij.aidebugger.evaluation.models.evaluators.EvaluationRunner
import com.intellij.aidebugger.evaluation.models.evaluators.EvaluatorEntry
import com.intellij.aidebugger.evaluation.models.evaluators.EvaluatorFactory
import com.intellij.aidebugger.evaluation.models.evaluators.evalAggregate
import com.intellij.aidebugger.evaluation.models.extractor.DebuggerTracesExtractor
import com.intellij.aidebugger.evaluation.models.llm.LlmProviderConfig
import com.intellij.aidebugger.evaluation.models.llm.createLlmProvider
import com.intellij.aidebugger.evaluation.models.repositories.EvalConfigsRepositoryImpl
import com.intellij.aidebugger.evaluation.models.repositories.EvaluationResultsRepositoryImpl
import com.intellij.aidebugger.evaluation.models.repositories.TableRow
import com.intellij.aidebugger.evaluation.models.repositories.TableSnapshot
import com.intellij.aidebugger.evaluation.models.runner.AiDebuggerGateway
import com.intellij.aidebugger.evaluation.models.runner.RunRequest
import com.intellij.aidebugger.evaluation.models.runner.TracerRunner
import com.intellij.aidebugger.evaluation.models.storage.sanitizeNameForFile
import com.intellij.aidebugger.evaluation.settings.AIToolkitSettingsService
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.full.memberProperties

@Service(Service.Level.PROJECT)
class LocalEvaluationService(private val project: Project) {
    private val LOG = Logger.getInstance(LocalEvaluationService::class.java)

    private val resultsRepository = project.service<EvaluationResultsRepositoryImpl>()
    private val tracesDatasetsRepository = project.service<TracesDatasetsRepository>()
    private val configsRepository = project.service<EvalConfigsRepositoryImpl>()
    private val settingsService = service<AIToolkitSettingsService>()
    private val persistenceService = project.service<EvaluationPersistenceService>()

    private val baseOut by lazy {
        project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
    }

    private val CONFIG_SELECTED_KEY: String = "com.intellij.aidebugger.evaluationView.selectedConfig.v1"

    interface Listener {
        fun onStatusChange(status: String)
        fun onTableSnapshotUpdate(snapshot: TableSnapshot)
        fun onError(@NlsContexts.DialogTitle title: String, @NlsContexts.DialogMessage message: String)
        fun onWarning(@NlsContexts.DialogMessage message: String)
    }

    private var inlineEvalActive: Boolean = false
    private var inlineEvalScope: CoroutineScope? = null
    private val inlineJobs: MutableList<Job> = mutableListOf()
    private val inlineResults: MutableList<EvalResult> = Collections.synchronizedList(mutableListOf<EvalResult>())
    // Refactored to use EvaluatorEntry to support type-based aggregation
    private var inlineEvaluators: List<EvaluatorEntry> = emptyList()

    private var tracerCancelFlag: AtomicBoolean? = null
    private var externalCancel: (() -> Unit)? = null

    private val lastRunDataPointsByKey: MutableMap<String, List<DataPoint>> = mutableMapOf()

    private var currentIds: List<String> = emptyList()
    private var currentRows: MutableList<TableRow> = mutableListOf()
    private var currentOutputs: MutableList<String> = mutableListOf()

    suspend fun startEvaluation(
        configName: String,
        listener: Listener
    ) = withContext(Dispatchers.IO) {
        listener.onStatusChange("Preparing…")
        val canceled = AtomicBoolean(false)
        tracerCancelFlag = AtomicBoolean(false)
        externalCancel = { canceled.set(true); listener.onStatusChange("Canceled") }

        try {
            val evalCfg = loadConfig(configName)
            if (evalCfg == null) {
                listener.onWarning(EvaluationBundle.message("eval.local.eval.warning.config.not.found", configName))
                listener.onStatusChange("Idle")
                return@withContext
            }

            listener.onStatusChange("Collecting dataset…")
            val datasetPathForInputs = resolveDatasetPath(evalCfg.datasetName)
            val inputs = if (datasetPathForInputs != null) {
                loadInputsFromDataset(datasetPathForInputs, evalCfg.datasetName ?: "")
            } else null

            if (inputs != null) {
                val specs = createInputSpecs(inputs, configName)
                initializeTableState(specs.map { it.id }, specs.map { it.input }, listener)
            } else {
                listener.onStatusChange("Dataset is empty or could not be loaded…")
            }

            val datasetPath = collectDatasetAsync(evalCfg, canceled, listener, configName)

            if (datasetPath == null) {
                listener.onWarning(EvaluationBundle.message("eval.local.eval.warning.no.dataset"))
                listener.onStatusChange("No dataset")
                return@withContext
            }

            if (canceled.get()) {
                listener.onStatusChange("Canceled")
                return@withContext
            }

            externalCancel = null

            if (inlineEvalActive) {
                listener.onStatusChange("Finalizing…")
                finalizeInlineEvaluationIfActiveAsync(configName, listener)?.join()
                listener.onStatusChange("Done")
            } else {
                listener.onStatusChange("Exiting")
            }

        } catch (e: EvaluationException) {
            LOG.warn("Evaluation failed: ${e.error}", e)
            listener.onError(EvaluationBundle.message("eval.error.title"), EvaluationBundle.message(e.error.key))
            stopEvaluation(listener, configName)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            LOG.warn("Run Eval error", t)
            listener.onError(EvaluationBundle.message("eval.local.eval.title"), t.message ?: t.toString())
            listener.onStatusChange("Failed")
        } finally {
            externalCancel = null
        }
    }

    fun stopEvaluation(listener: Listener?, configName: String?) {
        listener?.onStatusChange("Canceled")

        if (configName != null) {
            finalizeTableStateOnStop(configName, listener)
        }

        runCatching { inlineEvalScope?.cancel(CancellationException("Stopped")) }

        if (inlineEvalActive && configName != null) {
            CoroutineScope(Dispatchers.IO).launch {
                finalizeInlineEvaluationIfActiveAsync(configName, listener)?.join()
            }
        }

        externalCancel?.let { runCatching { it.invoke() } }
        try { tracerCancelFlag?.set(true) } catch (_: Throwable) { }

        resetInlineState()
    }

    private fun initializeTableState(ids: List<String>, inputs: List<String>, listener: Listener) {
        currentIds = ids
        currentRows = inputs.map { TableRow(it, emptyMap(), emptyMap()) }.toMutableList()
        currentOutputs = inputs.map { "running..." }.toMutableList()

        notifySnapshot(listener)
    }

    private fun notifySnapshot(listener: Listener?) {
        if (listener == null) return
        val snapshot = TableSnapshot(currentIds, currentRows.toList(), currentOutputs.toList())
        listener.onTableSnapshotUpdate(snapshot)
    }

    private fun loadConfig(name: String): EvalRunConfig? {
        val info = configsRepository.listConfigs().firstOrNull { it.name == name } ?: return null
        return runCatching { configsRepository.loadConfig(info) }.getOrNull()
    }

    private fun collectDatasetAsync(
        evalCfg: EvalRunConfig,
        cancelFlag: AtomicBoolean,
        listener: Listener,
        configName: String
    ): Path? {
        try {
            val dsRef = evalCfg.datasetName?.trim().orEmpty()
            if (dsRef.isBlank()) return null

            if (isConcreteDatasetPath(dsRef)) {
                val p = Path.of(dsRef)
                if (isDataPointsFile(p)) return p

                val info = resolveTracesDatasetByPath(dsRef)
                if (info != null) {
                    val datasetRows = tracesDatasetsRepository.loadDataset(info)
                    val pairs = datasetRows.map { row -> row.input.orEmpty() to row.output.orEmpty() }
                    return runTracerForPairs(pairs, evalCfg.runConfigName, cancelFlag, listener, configName)
                }
                return p
            }

            val info = tracesDatasetsRepository.listDatasets().firstOrNull { it.name == dsRef } ?: return null
            val datasetRows = tracesDatasetsRepository.loadDataset(info)
            val pairs = datasetRows.map { row -> row.input.orEmpty() to row.output.orEmpty() }
            return runTracerForPairs(pairs, evalCfg.runConfigName, cancelFlag, listener, configName)
        } catch (e: EvaluationException) {
            throw e
        } catch (e: Exception) {
            throw EvaluationException(EvaluationError.DATASET_READ_ERROR, e)
        }
    }

    private fun runTracerForPairs(
        rows: List<Pair<String, String>>,
        runConfigName: String,
        cancelFlag: AtomicBoolean,
        listener: Listener,
        configName: String
    ): Path? {
        val basePath = project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
        val runnerConfig = RunnerConfigRepository.loadRunnerConfig(basePath, runConfigName)

        val inputStructJson = runnerConfig?.inputStruct?.let {
            RunnerConfigRepository.gson.toJson(it)
        }

        val specs = rows.mapNotNull { (input, expected) ->
            val inp = input.trim()
            if (inp.isEmpty()) null else InputSpec(
                id = "dp_cfg_${System.currentTimeMillis()}_${(0..999999).random()}",
                input = inp,
                expectedOutput = expected.trim().takeIf { it.isNotEmpty() },
                inputStruct = inputStructJson
            )
        }

        if (specs.isEmpty()) return null

        initializeTableState(specs.map { it.id }, specs.map { it.input }, listener)

        val resolvedPath = runnerConfig?.mapping?.output?.trim()?.takeIf { it.isNotEmpty() } ?: "$"
        val inputPath = runnerConfig?.mapping?.input?.trim()?.takeIf { it.isNotEmpty() }
        val extractor = DebuggerTracesExtractor(resolvedPath)

        val evalConfig = loadConfig(configName) ?: return null

        try {
            val result = TracerRunner(
                AiDebuggerGateway(project, runConfigName),
                project.basePath ?: System.getProperty("user.dir"),
                extractor,
                inputPath
            ).run(
                RunRequest(
                    inputSpecs = specs,
                    cancelFlag = cancelFlag,
                    onDataPointOutput = { id, output -> updateOutputForId(id, output, listener) },
                    onDataPoint = { dp ->
                        if (dp.exception.isNullOrEmpty()) {
                            runSingleDataPointEvaluationAsync(evalConfig, dp, listener)
                        }
                    }
                )
            )
            return copyDatasetToConfigFile(result.datasetPath, configName)
        } catch (e: Exception) {
            throw EvaluationException(EvaluationError.AGENT_RUN_ERROR, e)
        }
    }

    private fun updateOutputForId(id: String, output: String, listener: Listener) {
        ApplicationManager.getApplication().invokeLater {
            try {
                if (tracerCancelFlag?.get() == true) return@invokeLater
                val idx = currentIds.indexOf(id)
                if (idx >= 0 && idx < currentOutputs.size) {
                    currentOutputs[idx] = output
                    notifySnapshot(listener)
                }
            } catch (_: Throwable) { }
        }
    }

    private fun runSingleDataPointEvaluationAsync(cfg: EvalRunConfig, dataPoint: DataPoint, listener: Listener) {
        if (tracerCancelFlag?.get() == true) return

        try {
            initializeInlineEvalIfNeeded(cfg)
        } catch (e: EvaluationException) {
            listener.onError(EvaluationBundle.message("eval.error.title"), EvaluationBundle.message(e.error.key))
            return
        } catch (e: Throwable) {
            LOG.warn("Failed to init inline eval", e)
            return
        }

        val evaluators = inlineEvaluators
        if (evaluators.isEmpty()) return
        val scope = inlineEvalScope ?: return
        val id = dataPoint.id

        val job = scope.launch(Dispatchers.IO) {
            try {
                if (tracerCancelFlag?.get() == true) return@launch

                updateRow(id) { row ->
                    val evaluatingScores = if (evaluators.isNotEmpty()) {
                        mapOf(evaluators.first().name to "evaluating…")
                    } else emptyMap()
                    TableRow(row.input, row.evaluatorScores + evaluatingScores, row.evaluatorExtras)
                }
                notifySnapshot(listener)

                val evalResults = EvaluationRunner.evaluateDataPoint(dataPoint, evaluators)

                if (tracerCancelFlag?.get() != true) {
                    inlineResults.addAll(evalResults.values)
                }

                val (scoreMap, extraMap) = EvaluationRunner.extractScoreAndExtraMaps(evalResults)
                val extraTextMap = evalResults.mapValues { (_, result) -> result.extractExtraText() }

                updateRow(id) { row ->
                    val mergedExtras = extraMap.toMutableMap().apply {
                        extraTextMap.forEach { (evalName, text) ->
                            if (text.isNotEmpty()) this[evalName] = text
                        }
                    }
                    TableRow(row.input, scoreMap, mergedExtras)
                }
                notifySnapshot(listener)

            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                LOG.warn("Error evaluating datapoint $id", t)
                val isApiKeyError = t.message?.contains("401") == true || t.message?.contains("api key", ignoreCase = true) == true
                if (isApiKeyError) {
                    ApplicationManager.getApplication().invokeLater {
                        listener.onError(EvaluationBundle.message("eval.error.title"), EvaluationBundle.message(EvaluationError.API_KEY_ERROR.key))
                    }
                }
            }
        }
        inlineJobs.add(job)
    }

    private fun updateRow(id: String, transform: (TableRow) -> TableRow) {
        val idx = currentIds.indexOf(id)
        if (idx in currentRows.indices) {
            currentRows[idx] = transform(currentRows[idx])
        }
    }

    private fun initializeInlineEvalIfNeeded(cfg: EvalRunConfig) {
        if (inlineEvalActive && inlineEvaluators.isNotEmpty()) return

        var llmProviderCredentials = settingsService.getCredentialsById(cfg.providerInstanceId)
        if (llmProviderCredentials == null) {
            llmProviderCredentials = settingsService.getCredentialsProviderType(cfg.providerType)
            if (llmProviderCredentials != null) {
                val selectedName = getSelectedConfigName()
                if (!selectedName.isNullOrBlank()) {
                    val updatedConfig = cfg.copy(providerInstanceId = llmProviderCredentials.id)
                    configsRepository.createOrUpdateConfig(selectedName, updatedConfig)
                }
            }
        }

        if (llmProviderCredentials == null) {
            throw EvaluationException(EvaluationError.LLM_PROVIDER_NOT_FOUND)
        }

        val apiKey = llmProviderCredentials.apiKey
        if (apiKey.isBlank()) {
            throw EvaluationException(EvaluationError.API_KEY_ERROR)
        }

        val config = LlmProviderConfig(
            apiKey = apiKey,
            modelName = cfg.modelName ?: "",
            temperature = cfg.modelParams?.get("temperature")?.toDoubleOrNull() ?: 0.0,
            providerType = llmProviderCredentials.providerType
        )

        val provider = createLlmProvider<LLMScore>(config)
        val inputVars = extractInputVarsFromTemplate(cfg.promptTemplate) ?: defaultInputVars()

        inlineEvaluators = cfg.evaluators!!.map { evaluatorConfig ->
            EvaluatorEntry(
                name = evaluatorConfig.name,
                type = evaluatorConfig.type,
                instance = EvaluatorFactory.createFromConfig(
                    evaluatorConfig,
                    provider,
                    inputVars
                )
            )
        }

        inlineEvalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        inlineJobs.clear()
        inlineResults.clear()
        inlineEvalActive = true
    }

    private fun finalizeInlineEvaluationIfActiveAsync(configName: String, listener: Listener?): Job? {
        if (!inlineEvalActive) return null
        val jobs = inlineJobs.toList()
        val outer = CoroutineScope(Dispatchers.IO)
        return outer.launch(Dispatchers.IO) {
            try {
                runCatching { jobs.joinAll() }

                val evalCfg = loadConfig(configName)
                if (evalCfg != null) {
                    reportEvalFinishedToFUS(evalCfg)
                }

                val completed = inlineResults.toList()
                if (completed.isEmpty()) return@launch

                val expId = completed.groupingBy { it.experimentId }.eachCount().maxByOrNull { it.value }?.key
                    ?: ("exp_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")))
                val agg = evalAggregate(completed, expId)

                val outDir = baseOut.resolve(".jbeval/eval")
                runCatching { resultsRepository.saveEvaluationResult(agg, completed, outDir, configName) }

                val snapshot = TableSnapshot(currentIds, currentRows.toList(), currentOutputs.toList(), agg.evaluatorsStats)
                persistenceService.saveTableSnapshot(configName, snapshot)

            } finally {
                resetInlineState()
            }
        }
    }

    private fun finalizeTableStateOnStop(configName: String, listener: Listener?) {
        for (i in currentOutputs.indices) {
            if (currentOutputs[i].equals("running...", ignoreCase = true)) {
                currentOutputs[i] = ""
            }
        }
        for (i in currentRows.indices) {
            val row = currentRows[i]
            val cleanScores = row.evaluatorScores.mapValues { (_, score) ->
                if (isPlaceholderValue(score)) "" else score
            }
            val cleanExtras = row.evaluatorExtras.mapValues { (_, extra) ->
                if (isPlaceholderValue(extra)) "" else extra
            }
            val cleared = currentOutputs[i].isBlank()
            val finalScores = if (cleared) emptyMap() else cleanScores
            val finalExtras = if (cleared) emptyMap() else cleanExtras
            currentRows[i] = TableRow(row.input, finalScores, finalExtras)
        }

        notifySnapshot(listener)
        val snapshot = TableSnapshot(currentIds, currentRows.toList(), currentOutputs.toList())
        persistenceService.saveTableSnapshot(configName, snapshot)
    }

    private fun isPlaceholderValue(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val normalized = value.trim().lowercase()
        return normalized.startsWith("evaluating") || normalized.startsWith("running")
    }

    private fun resetInlineState() {
        inlineEvalActive = false
        runCatching { inlineEvalScope?.cancel() }
        inlineEvalScope = null
        inlineJobs.clear()
        inlineResults.clear()
        inlineEvaluators = emptyList()
    }

    private fun copyDatasetToConfigFile(srcPath: String, configName: String): Path {
        val sanitized = sanitizeNameForFile(configName)
        val evalDir = baseOut.resolve(".jbeval").resolve("eval")
        Files.createDirectories(evalDir)

        val target = evalDir.resolve("${sanitized}.json")
        val src = Path.of(srcPath)

        runCatching { Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING) }
            .recoverCatching { Files.move(src, target, StandardCopyOption.REPLACE_EXISTING) }

        runCatching { if (src != target) Files.deleteIfExists(src) }

        return target
    }

    private fun resolveDatasetPath(datasetRef: String?): Path? {
        val dsRef = datasetRef?.trim().orEmpty()
        if (dsRef.isBlank()) return null

        if (isConcreteDatasetPath(dsRef)) {
            val p = Path.of(dsRef)
            if (Files.exists(p) && Files.isRegularFile(p)) return p

            val info = resolveTracesDatasetByPath(dsRef)
            if (info != null) {
                val base = project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
                return base.resolve(".jbeval").resolve("datasets").resolve(info.fileName)
            }
            return null
        }

        val info = tracesDatasetsRepository.listDatasets().firstOrNull { it.name == dsRef }
        if (info != null) {
            val base = project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
            return base.resolve(".jbeval").resolve("datasets").resolve(info.fileName)
        }

        val base = project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
        val fileName = if (dsRef.lowercase().endsWith(".json")) dsRef else "$dsRef.json"
        val fallbackPath = base.resolve(".jbeval").resolve("datasets").resolve(fileName)
        return if (Files.exists(fallbackPath) && Files.isRegularFile(fallbackPath)) fallbackPath else null
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

            val rows = tracesDatasetsRepository.loadDataset(info, null, null) // Simplified call, ignoring mapping overrides for initial load
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

    private fun createInputSpecs(inputs: List<String>, configName: String): List<InputSpec> {
        val evalCfg = loadConfig(configName)
        val runConfigName = evalCfg?.runConfigName
        val inputStructJson = if (!runConfigName.isNullOrBlank()) {
            val basePath = project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
            val runnerConfig = RunnerConfigRepository.loadRunnerConfig(basePath, runConfigName)
            runnerConfig?.inputStruct?.let { RunnerConfigRepository.gson.toJson(it) }
        } else null

        return inputs.mapNotNull { input ->
            val inp = input.trim()
            if (inp.isEmpty()) null else InputSpec(
                id = "dp_cfg_${System.currentTimeMillis()}_${(0..999999).random()}",
                input = inp,
                expectedOutput = null,
                inputStruct = inputStructJson
            )
        }
    }

    private fun isConcreteDatasetPath(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        return runCatching {
            val p = Path.of(value)
            Files.exists(p) && Files.isRegularFile(p)
        }.getOrDefault(false)
    }

    private fun isDataPointsFile(path: Path): Boolean {
        return runCatching {
            val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            Files.newBufferedReader(path).use { br ->
                mapper.readValue(br, Array<DataPoint>::class.java) != null
            }
        }.getOrDefault(false)
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

    private fun extractInputVarsFromTemplate(template: String?): Map<String, String>? {
        if (template.isNullOrBlank()) return null
        val varRegex = Regex("\\{([a-zA-Z0-9_]+)\\}")
        val vars = varRegex.findAll(template).map { it.groupValues[1] }.toSet()
        return if (vars.isEmpty()) null else vars.associateWith { it }
    }

    private fun defaultInputVars(): Map<String, String> =
        DataPoint::class.memberProperties
            .filter { it.name in setOf("input", "outputExpected", "outputGen") }
            .associate { it.name to it.name }

    private fun getSelectedConfigName(): String? {
        return PropertiesComponent.getInstance(project).getValue(CONFIG_SELECTED_KEY)?.takeIf { it.isNotBlank() }
    }

    private fun reportEvalFinishedToFUS(evalCfg: EvalRunConfig) {
        val completed = inlineResults.toList()
        val rowsCount = completed.size
        val errorsCount = completed.filter { it.extra.keys.contains("error") }.size
        val evaluatorsCount = evalCfg.evaluators?.size ?: 0
        val modelProvider = evalCfg.providerType
        val hasLLMJudge = evalCfg.evaluators?.any { it.type == "llm judge" } ?: false
        val hasRegex = evalCfg.evaluators?.any { it.type == "regex" } ?: false

        EvaluationCollector.reportEvalRunFinished(
            project,
            rowsCount,
            errorsCount,
            evaluatorsCount,
            modelProvider,
            hasLLMJudge,
            hasRegex,
        )
    }
}