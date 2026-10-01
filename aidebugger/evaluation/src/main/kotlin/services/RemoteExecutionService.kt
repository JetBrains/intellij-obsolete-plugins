package com.intellij.aidebugger.evaluation.services

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.intellij.aidebugger.common.models.JsonPathDefaults
import com.intellij.aidebugger.common.models.RunnerConfigRepository
import com.intellij.aidebugger.common.utility.JsonPathUtils
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.EvalResult
import com.intellij.aidebugger.evaluation.models.entities.extractExtraText
import com.intellij.aidebugger.evaluation.models.remote.RemoteRunStateService
import com.intellij.aidebugger.evaluation.models.storage.JsonFileStorage
import com.intellij.aidebugger.evaluation.models.storage.sanitizeNameForFile
import com.intellij.aidebugger.evaluation.viewModels.CONFIG_SELECTED_KEY
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.ide.BuiltInServerManager
import java.io.IOException
import java.math.RoundingMode
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

private val LOG = Logger.getInstance("RemoteExecutionService")

data class ProvisioningConf(
    @JsonProperty("gpu_type", required = false) val gpuType: String?,
    @JsonProperty("gpu_count", required = false) val gpuCount: Int?,
    @JsonProperty("cpu_count", required = false) val cpuCount: Int?,
    @JsonProperty("ram", required = false) val ram: Int?
)

/**
 * Request structure for starting remote execution.
 */
data class RemoteExecutionRequest(
    val name: String?,
    val root: String?,
    val workingDir: String?,
    val command: String?,
    val dockerImage: String?,
    val dockerAdditionalArgs: String?,
    val includesPath: List<String>?,
    val outputPath: String?,
    val pythonVersion: String?,
    val pipRequirementsPath: String?,
    val poetryDirPath: String?,
    val variables: Map<String, String>?,
    val secretVariables: Map<String, String>?,
    val runConfigName: String?,
    val provisioningConf: ProvisioningConf?
)

/**
 * Status information for a remote execution.
 */
data class RemoteExecutionStatus(
    val id: String,
    val status: String,
    val isTerminal: Boolean
)

data class CadenceAuthToken(
    val token: String
)

/**
 * Parsed results from remote execution output.
 */
data class RemoteExecutionResult(
    val meanScore: String?,
    val resultsByInput: Map<String, Pair<Map<String, String>, Map<String, String>>>,
    val outputsByInput: Map<String, String>
)

@Suppress("UnresolvedPluginConfigReference")
private fun loginToCadence(): AnActionResult {
    val logInAction =
        ActionUtil.getAction("org.jetbrains.jettrain.actions.JbaAuthAction") ?: error("Wrong version of Cadence is installed")

    return ActionUtil.performAction(
        logInAction,
        AnActionEvent.createEvent(DataContext.EMPTY_CONTEXT, Presentation(), ActionPlaces.UNKNOWN, ActionUiKind.POPUP, null)
    )
}

/**
 * Service responsible for managing remote evaluation execution.
 * Handles starting, polling, stopping, and downloading results from remote execution endpoints.
 */
@Service(Service.Level.PROJECT)
class RemoteExecutionService(private val project: Project) : Disposable {

    private val builtInServerPort
        get() = BuiltInServerManager.getInstance().port

    private val httpClient = OkHttpClient()
    private val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val pollScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // One network polling loop per execution id and allow multiple listeners to attach
    private data class PollEntry(
        val job: Job,
        val listeners: MutableList<Pair<(String) -> Unit, () -> Unit>>,
        @Volatile var lastStatus: String? = null
    )

    private val activePolls: MutableMap<String, PollEntry> = mutableMapOf()

    private val cadenceAuthToken = AtomicReference<String>(null)

    /**
     * Cancel local polling for the given execution id
     * Use [stop] to stop the remote run itself.
     */
    fun cancelPolling(executionId: String) {
        synchronized(activePolls) {
            activePolls.remove(executionId)?.job?.cancel()
        }
    }
    /**
     * Cancel all local polling jobs
     * Does not clear persisted RemoteRunStateService state so runs can be resumed later.
     */
    fun cancelAllPolling() {
        val jobs: List<Job> = synchronized(activePolls) {
            val list = activePolls.values.map { it.job }
            activePolls.clear()
            list
        }
        jobs.forEach { runCatching { it.cancel() } }
    }

    /**
     * Start remote execution and return (executionId, pollingJob).
     *
     * @param request The remote execution request configuration
     * @param pollIntervalMs Interval between status polls in milliseconds
     * @param onStatusUpdate Callback invoked when status changes
     * @param onTerminal Callback invoked when execution reaches terminal state
     * @return Pair of execution ID and polling job
     */
    fun startAndPoll(
        request: RemoteExecutionRequest,
        pollIntervalMs: Long = 4000L,
        onStatusUpdate: (String) -> Unit = {},
        onTerminal: () -> Unit = {}
    ): Pair<String, Job> {
        runInEdt {
            PropertiesComponent.getInstance().setValue("cadence.onboarding.tour", true)
            PropertiesComponent.getInstance().setValue("cadence.jba.auth.shortcut.shown", true)
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // Ensure Cadence plugin is installed
        val pluginInstaller = project.service<PluginDownloaderService>()
        val pluginInstallJob = scope.launch {
            if (!pluginInstaller.isCadencePluginInstalled()) {
                withContext(Dispatchers.Main) {
                    onStatusUpdate("Installing Cadence plugin…")
                }
                try {
                    pluginInstaller.installCadencePlugin(project)
                    while (!pluginInstaller.isCadencePluginInstalled()) {
                        if (project.isDisposed || !isActive) break
                        delay(500)
                    }
                    withContext(Dispatchers.Main) {
                        onStatusUpdate("Remote eval: starting…")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onStatusUpdate("Remote eval: Failed (plugin not installed)")
                    }
                    throw IllegalStateException("Cadence plugin installation failed", e)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onStatusUpdate("Remote eval: starting…")
                }
            }
        }

        // If the project is closing while waiting for plugin installation, cancel the job
        project.messageBus.connect(project).subscribe(
            ProjectManager.TOPIC,
            object : ProjectManagerListener {
                override fun projectClosing(closingProject: Project) {
                    if (closingProject == project) {
                        pluginInstallJob.cancel(CancellationException("Project is closing; cancel plugin installation"))
                    }
                }
            }
        )

        // Wait for plugin installation to complete
        runBlocking { pluginInstallJob.join() }

        if (pluginInstallJob.isCancelled || project.isDisposed) {
            throw CancellationException("Plugin installation cancelled by user")
        }

        runInEdt {
            loginToCadence()
        }

        val cadenceAuthToken = getCadenceAuthToken()

        // Start remote execution
        val startUrl = remoteUrl("/start")
        val gson = Gson()
        val jsonBody = gson.toJson(request)
        val startReq = Request.Builder()
            .url(startUrl)
            .header("X-AIToolkit-Allowed", "true")
            .addAuthHeader(cadenceAuthToken)
            .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val executionId: String = try {
            httpClient.newCall(startReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                try {
                    val json = JsonParser.parseString(body).asJsonObject
                    json.get("id")?.asString
                        ?: throw IllegalStateException("id missing in response")
                } catch (t: Throwable) {
                    throw IllegalStateException("Failed to start remote evaluation: ${t.message ?: t}")
                }
            }
        } catch (t: Throwable) {
            onStatusUpdate("Failed to start")
            throw t
        }

        val selectedName =
            PropertiesComponent.getInstance(project).getValue(CONFIG_SELECTED_KEY)?.takeIf { it.isNotBlank() }!!
        // Persist execution ID and eval config name to resume after restart
        RemoteRunStateService.getInstance(project).save(executionId, selectedName)

        val pollingJob = pollStatus(
            executionId = executionId,
            pollIntervalMs = pollIntervalMs,
            onStatusUpdate = onStatusUpdate,
            onTerminal = onTerminal
        )

        return executionId to pollingJob
    }

    fun pollStatus(
        executionId: String,
        pollIntervalMs: Long = 4000L,
        onStatusUpdate: (String) -> Unit = {},
        onTerminal: () -> Unit = {}
    ): Job {
        // If a poller for this executionId already exists, attach listeners
        synchronized(activePolls) {
            val existing = activePolls[executionId]
            if (existing != null) {
                existing.listeners += (onStatusUpdate to onTerminal)
                existing.lastStatus?.let { status ->
                    runCatching { onStatusUpdate(status) }
                }
                return existing.job
            }
        }

        val statusUrl = remoteUrl("/status", "id=${enc(executionId)}")
        val statusReq = allowedGet(statusUrl, getCadenceAuthToken())

        val job = pollScope.launch {
            var prevStatus = "Starting remote..."
            while (isActive) {
                val status = try {
                    httpClient.newCall(statusReq).execute().use { resp ->
                        val body = resp.body?.string().orEmpty()
                        try {
                            val json = JsonParser.parseString(body).asJsonObject
                            json.get("status")?.asString ?: body
                        } catch (_: Throwable) {
                            body
                        }
                    }
                } catch (t: Throwable) {
                    "error: ${t.message ?: t}"
                }

                // Skip transient errors during startup
                if (status.contains("error") && prevStatus.contains("Starting", true)) {
                    delay(pollIntervalMs)
                    continue
                }

                // Handle rate limiting
                if (status.lowercase().contains("too many requests") || status.lowercase().contains("timeout")) {
                    delay(pollIntervalMs)
                    continue
                }

                // Update status if changed
                if (status != prevStatus) {
                    val listeners =
                        synchronized(activePolls) { activePolls[executionId]?.listeners?.toList() ?: emptyList() }
                    listeners.forEach { (cb, _) -> runCatching { cb(status) } }
                    // Remember last status for late subscribers
                    synchronized(activePolls) { activePolls[executionId]?.lastStatus = status }
                    prevStatus = status
                }

                // Check for terminal states
                val term = status.lowercase()
                if (term.contains("failed")) {
                    val listeners =
                        synchronized(activePolls) { activePolls[executionId]?.listeners?.toList() ?: emptyList() }
                    listeners.forEach { (cb, _) -> runCatching { cb(status) } }
                    listeners.forEach { (_, termCb) -> runCatching { termCb() } }
                    synchronized(activePolls) { activePolls.remove(executionId) }
                    return@launch
                }
                if (term.contains("finished") || term.contains("canceled")) {
                    val listeners =
                        synchronized(activePolls) { activePolls[executionId]?.listeners?.toList() ?: emptyList() }
                    listeners.forEach { (cb, _) -> runCatching { cb(status) } }
                    delay(3000L) // Give server time to finalize outputs
                    listeners.forEach { (_, termCb) -> runCatching { termCb() } }
                    synchronized(activePolls) { activePolls.remove(executionId) }
                    return@launch
                }

                delay(pollIntervalMs)
            }
        }

        // Register new poller after starting the job
        synchronized(activePolls) {
            val existing = activePolls[executionId]
            if (existing == null) {
                activePolls[executionId] = PollEntry(job, mutableListOf(onStatusUpdate to onTerminal))
            } else {
                existing.listeners += (onStatusUpdate to onTerminal)
                existing.lastStatus?.let { status -> runCatching { onStatusUpdate(status) } }
                return existing.job
            }
        }

        job.invokeOnCompletion {
            synchronized(activePolls) {
                activePolls[executionId]?.let { entry ->
                    if (entry.job == job) {
                        activePolls.remove(executionId)
                    }
                }
            }
        }

        return job
    }

    override fun dispose() {
        cancelAllPolling()
    }

    fun getCadenceAuthToken(): String {
        if (cadenceAuthToken.get() != null) {
            return cadenceAuthToken.get()
        }
        val authUrl = remoteUrl("/auth")
        val request = allowedGet(authUrl, null)
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to access Cadence")
            }
            try {
                val body = mapper.readValue(response.body?.string(), CadenceAuthToken::class.java)
                cadenceAuthToken.set(body.token)
                return body.token
            } catch (e: Exception) {
                throw IllegalStateException("Failed to access Cadence")
            }
        }
    }

    /**
     * Stop remote execution by ID.
     *
     * @param executionId The ID of the execution to stop
     */
    suspend fun stop(executionId: String) {
        val stopUrl = remoteUrl("/stop", "id=${enc(executionId)}")

        withContext(Dispatchers.IO) {
            val stopReq = allowedGet(stopUrl, getCadenceAuthToken())
            try {
                httpClient.newCall(stopReq).execute().use { response ->
                    if (!response.isSuccessful) {
                        LOG.warn("Failed to stop remote execution $executionId: ${response.code}")
                    }
                }
            } catch (e: IOException) {
                LOG.warn("Failed to stop remote execution $executionId", e)
                throw e
            }
        }
    }


    /**
     * Download outputs from remote execution.
     *
     * @param executionId The ID of the execution
     * @param targetPath Local path to download outputs to
     * @param evalConfigName Name of the eval config (used for final storage location)
     * @return Path to the final resolved location where artifacts are stored
     */
    suspend fun downloadOutputs(executionId: String, targetPath: Path, evalConfigName: String): Path {
        cleanupTargetPath(targetPath)
        downloadRemoteResults(executionId, targetPath)
        val resolvedPath = copyResultsToEvalDir(targetPath, evalConfigName)
        cleanupTargetPath(targetPath)
        return resolvedPath
    }

    private fun cleanupTargetPath(targetPath: Path) {
        if (Files.exists(targetPath)) {
            JsonFileStorage.deleteRecursively(targetPath)
        }
    }

    private suspend fun downloadRemoteResults(executionId: String, targetPath: Path) {
        LOG.info("Downloading remote results for execution $executionId to $targetPath")
        val query = "id=${enc(executionId)}&downloadToPath=${enc(targetPath.toString())}"
        val downloadUrl = remoteUrl("/download", query)

        withContext(Dispatchers.IO) {
            val downloadReq = allowedGet(downloadUrl, getCadenceAuthToken())
            try {
                httpClient.newCall(downloadReq).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Download failed: ${response.code} ${response.message}")
                    }
                }
            } catch (e: IOException) {
                LOG.warn("Failed to download remote outputs for $executionId", e)
                throw e
            }
        }
    }

    private fun copyResultsToEvalDir(sourcePath: Path, evalConfigName: String): Path {
        val sanitized = sanitizeNameForFile(evalConfigName)
        val basePath = project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
        val evalDir = basePath.resolve(".jbeval").resolve("eval").resolve(sanitized)

        val nestedSource = sourcePath.resolve(".jbeval").resolve("eval").resolve(sanitized)
        val actualSource = when {
            Files.exists(nestedSource) -> nestedSource
            Files.exists(sourcePath) -> sourcePath
            else -> null
        }
        if (actualSource != null) {
            if (Files.exists(evalDir)) {
                JsonFileStorage.deleteRecursively(evalDir)
            }
            Files.createDirectories(evalDir)
            JsonFileStorage.copyRecursively(actualSource, evalDir)
        }
        return evalDir
    }

    /**
     * Parse downloaded remote results from output directory.
     *
     * @param outputDir Directory containing downloaded results
     * @param runConfigName Run configuration name to resolve output JSON path
     * @return Parsed execution results
     */
    fun parseResults(outputDir: Path, runConfigName: String?): RemoteExecutionResult {
        val aggFile = outputDir.resolve("eval_result.json")
        val raw = outputDir.resolve("eval_results_raw.json")

        // Parse mean score from aggregated results
        var meanStr: String? = null
        runCatching {
            if (Files.exists(aggFile)) {
                val statsText = Files.readString(aggFile)
                val node = mapper.readTree(statsText)
                val mean = node.get("stats")?.get("mean")?.asDouble()
                if (mean != null) {
                    meanStr = formatScore(mean)
                }
            }
        }.onFailure { e ->
            LOG.warn("Failed to parse aggregated results", e)
        }

        val byId = mutableMapOf<String, Pair<Map<String, String>, Map<String, String>>>()
        val outputsById = mutableMapOf<String, String>()

        if (Files.exists(raw)) {
            val rawText = runCatching { Files.readString(raw) }.getOrNull()
            if (!rawText.isNullOrBlank()) {
                val parsedStrict = runCatching {
                    mapper.readValue(rawText, object : TypeReference<List<EvalResult>>() {})
                }.getOrNull()

                if (parsedStrict != null) {
                    val groupedById = parsedStrict.groupBy { it.id }

                    for ((id, results) in groupedById) {
                        if (results.isEmpty()) continue

                        val first = results.first()

                        val scoresMap = mutableMapOf<String, String>()
                        val extrasMap = mutableMapOf<String, String>()

                        for (res in results) {
                            scoresMap[res.evaluator] = formatScore(res.score)
                            val extraText = res.extractExtraText()
                            if (extraText.isNotBlank()) {
                                extrasMap[res.evaluator] = extraText
                            }
                        }

                        byId[id] = scoresMap to extrasMap

                        outputsById[id] = deriveDisplayOutput(
                            first.outputGen,
                            first.raw,
                            runConfigName
                        )
                    }

                    val dataPoints = parsedStrict.map { it.toDataPoint() }
                    saveForTracesInvestigation(outputDir, dataPoints)

                } else {
                    runCatching {
                        val list = mapper.readValue(rawText, List::class.java)
                        @Suppress("UNCHECKED_CAST")
                        (list as? List<Map<String, Any?>>)
                            ?.groupBy { (it["id"] as? String)?.trim().orEmpty() }
                            ?.forEach { (id, maps) ->
                                if (id.isBlank()) return@forEach

                                val first = maps.first()

                                val scoresMap = mutableMapOf<String, String>()
                                val extrasMap = mutableMapOf<String, String>()

                                for (m in maps) {
                                    val evaluatorName = (m["evaluator"] as? String) ?: "Evaluator-1"
                                    val scoreNum = (m["score"] as? Number)?.toDouble()
                                    if (scoreNum != null) {
                                        scoresMap[evaluatorName] = formatScore(scoreNum)
                                    }

                                    val extraMap = (m["extra"] as? Map<*, *>)
                                        ?.entries
                                        ?.associate { it.key.toString() to it.value }
                                    val explanation = when {
                                        extraMap?.get("explanation") is String -> extraMap["explanation"] as String
                                        else -> (m["explanation"] as? String).orEmpty()
                                    }
                                    if (explanation.isNotBlank()) {
                                        extrasMap[evaluatorName] = explanation
                                    }
                                }

                                byId[id] = scoresMap to extrasMap

                                outputsById[id] = deriveDisplayOutput(
                                    null,
                                    first,
                                    runConfigName
                                )
                            }
                    }.onFailure { e ->
                        LOG.warn("Failed to parse raw results", e)
                    }
                }
            }
        }

        return RemoteExecutionResult(
            meanScore = meanStr,
            resultsByInput = byId,
            outputsByInput = outputsById
        )
    }

    private fun saveForTracesInvestigation(outputDir: Path, dataPoints: List<DataPoint>) {
        runCatching {
            val basePath = project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
            val evalDir = basePath.resolve(".jbeval").resolve("eval")
            Files.createDirectories(evalDir)
            val outputDirName = outputDir.fileName.toString()
            val dataPointsFile = evalDir.resolve("${outputDirName}.json")
            val dataPointsJson = mapper.writeValueAsString(dataPoints)
            Files.writeString(dataPointsFile, dataPointsJson)
        }.onFailure { e ->
            LOG.warn("Failed to save dataPoints to file", e)
        }
    }

    private fun EvalResult.toDataPoint(): DataPoint =
        DataPoint(
            id = id,
            input = input,
            outputGen = outputGen,
            outputExpected = outputExpected,
            experimentId = experimentId,
            raw = raw,
            runDatetime = runDatetime,
            exception = null // todo
        )

    private fun remoteUrl(path: String, query: String? = null): String {
        val port = builtInServerPort
        val base = "http://localhost:$port/api/execution"
        return if (query.isNullOrBlank()) "$base$path" else "$base$path?$query"
    }

    private fun formatScore(score: Double, maxDecimals: Int = 3): String {
        val bigDecimal = score.toBigDecimal().setScale(maxDecimals, RoundingMode.HALF_UP).stripTrailingZeros()
        var result = bigDecimal.toPlainString()

        // Ensure at least one decimal place (e.g., "1" becomes "1.0")
        if (!result.contains('.')) {
            result = "$result.0"
        }

        return result
    }

    private fun enc(v: String): String =
        URLEncoder.encode(v, Charsets.UTF_8)

    private fun Request.Builder.addAuthHeader(token: String?) = apply {
        if (token != null) {
            header("Authorization", "Bearer $token")
        }
    }

    private fun allowedGet(url: String, token: String?): Request =
        Request.Builder()
            .url(url)
            .header("X-AIToolkit-Allowed", "true")
            .addAuthHeader(token)
            .get()
            .build()

    private fun deriveDisplayOutput(outputGen: String?, raw: Map<String, Any?>, runConfigName: String?): String {
        val og = outputGen?.trim().orEmpty()
        if (og.isNotEmpty()) {
            val je = asJsonElementOrNull(og)
            if (je == null) return og
            if (je.isJsonPrimitive && je.asJsonPrimitive.isString) return je.asString
            if (!(je.isJsonObject && je.asJsonObject.has("rootEvents"))) {
                return je.toString()
            }
        }

        // Fallback: extract from raw using dynamic path from runner config
        val fromRaw = extractLastAiMessageContentFromRaw(raw, runConfigName)
        if (fromRaw.isNotEmpty()) return fromRaw

        return og
    }

    private fun asJsonElementOrNull(s: String?): JsonElement? {
        if (s == null) return null
        val t = s.trim()
        if (t.isEmpty()) return null
        return try {
            JsonParser.parseString(t)
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractLastAiMessageContentFromRaw(raw: Map<String, Any?>, runConfigName: String?): String {
        return try {
            val gson = Gson()
            val root = gson.toJsonTree(raw)

            val path = runConfigName?.let {
                val basePath = project.basePath?.let { Path.of(it) }
                    ?: Path.of(System.getProperty("user.home"))
                val runnerConfig = RunnerConfigRepository.loadRunnerConfig(basePath, it)
                runnerConfig?.mapping?.output?.trim()?.takeIf { it.isNotEmpty() }
            } ?: JsonPathDefaults.DEFAULT_OUTPUT_PATH
            JsonPathUtils.extractAsString(root, path) ?: ""

        } catch (_: Throwable) {
            ""
        }
    }
}
