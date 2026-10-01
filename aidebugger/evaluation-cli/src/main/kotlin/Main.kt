package com.intellij.aidebugger.evaluationCli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.Strictness
import com.intellij.aidebugger.common.models.HierarchicalTraceEventsState
import com.intellij.aidebugger.common.models.JsonPathDefaults
import com.intellij.aidebugger.common.models.RunnerConfigRepository
import com.intellij.aidebugger.common.models.SessionCountersImpl
import com.intellij.aidebugger.common.models.buildHierarchicalStructure
import com.intellij.aidebugger.common.models.data.CsvDatasetReader
import com.intellij.aidebugger.common.models.data.JsonDatasetReader
import com.intellij.aidebugger.common.models.entities.SerializableTraceEvent
import com.intellij.aidebugger.common.models.serializeHierarchicalTraceEventsStateToMap
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.EvalRunConfig
import com.intellij.aidebugger.evaluation.models.entities.LLMScore
import com.intellij.aidebugger.evaluation.models.evaluators.EvaluationRunner
import com.intellij.aidebugger.evaluation.models.evaluators.EvaluatorEntry
import com.intellij.aidebugger.evaluation.models.evaluators.EvaluatorFactory
import com.intellij.aidebugger.evaluation.models.extractor.DebuggerTracesExtractor
import com.intellij.aidebugger.evaluation.models.extractor.FallbackContext
import com.intellij.aidebugger.evaluation.models.extractor.InputStructurePacker
import com.intellij.aidebugger.evaluation.models.llm.LlmProviderConfig
import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType
import com.intellij.aidebugger.evaluation.models.llm.UniversalApiKeyResolver
import com.intellij.aidebugger.evaluation.models.llm.createLlmProvider
import com.intellij.aidebugger.evaluation.models.storage.sanitizeNameForFile
import com.intellij.aidebugger.evaluation.models.storage.saveEvaluationResults
import com.intellij.aidebugger.python.models.CommonTraceEventsRepository
import com.intellij.aidebugger.python.serialization.CommonTraceEventsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.file.Files
import java.nio.file.Paths

class EvaluationCli : CliktCommand(name = "evaluation-cli") {
    override fun help(context: Context): String = "AI Toolkit evaluation command line interface"

    companion object {
        const val VERSION: String = "0.2.0"
    }

    override fun run() = Unit
}

class PipelineCommand : CliktCommand(name = "pipeline") {
    override fun help(context: Context): String = "Run trace → prepare-eval → make-config → run-eval in one go"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val projectDir = System.getProperty("user.dir") ?: "."
    private val outDir by option("--out-dir", help = "Directory for outputs (traces and eval outputs)").default(
        defaultOutDir()
    )

    private val preparedName: String? = detectPreparedConfigNameFromNext()
    private val dataset: String
        get() = Paths.get(projectDir, ".jbeval", "remote", "next", preparedName.toString(), "dataset.json").toString()
    private val script: String
        get() = Paths.get(projectDir, ".jbeval", "remote", "next", preparedName.toString(), "script.py").toString()

    private val evalRunConfig: EvalRunConfig? by lazy {
        loadEvalRunConfigFromNext(preparedName)
    }

    private val gson = GsonBuilder().setStrictness(Strictness.LENIENT).create()
    private val gsonPretty = GsonBuilder().setPrettyPrinting().setStrictness(Strictness.LENIENT).create()
    private val jacksonMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    private fun resolveExtractorOutputPath(): String {
        val projectPath = Paths.get(projectDir)
        val runConfigName = evalRunConfig?.runConfigName?.takeIf { it.isNotBlank() } ?: "main"

        val runnerConfig = RunnerConfigRepository.loadRunnerConfig(projectPath, runConfigName)

        echo("runner config: $runnerConfig")

        return runnerConfig?.mapping?.output?.trim()?.takeIf { it.isNotEmpty() }
            ?: JsonPathDefaults.DEFAULT_OUTPUT_PATH
    }

    private fun loadEvalRunConfigFromNext(preparedName: String?): EvalRunConfig? {
        if (preparedName.isNullOrBlank()) return null

        return try {
            // Try to load from remote/next location (where config is staged)
            val remotePath = Paths.get(".jbeval", "remote", "next", preparedName, "config.json")
            if (Files.exists(remotePath)) {
                jacksonMapper.readValue(remotePath.toFile(), EvalRunConfig::class.java)
            } else {
                // Fallback: try to load from configs directory
                val configPath = Paths.get(".jbeval", "configs", "$preparedName.json")
                if (Files.exists(configPath)) {
                    jacksonMapper.readValue(configPath.toFile(), EvalRunConfig::class.java)
                } else {
                    null
                }
            }
        } catch (e: Throwable) {
            echo("[pipeline] Warning: Could not load eval config: ${e.message}")
            null
        }
    }

    override fun run() {
        if (preparedName == null) {
            echo("[pipeline] Error: Could not detect prepared config name in .jbeval/remote/next/")
            throw RuntimeException("Config name not found")
        }

        val tracesPath = Paths.get(outDir, "tracer_outputs.json").toString()
        val extractorOutputPath = resolveExtractorOutputPath()

        echo("[pipeline] Tracing inputs from dataset…")
        val rows = readDataset(dataset)
        if (rows.isEmpty()) {
            echo("No inputs found in dataset: $dataset")
            return
        }
        val traceResults = runTracing(rows)
        jacksonMapper.writerWithDefaultPrettyPrinter()
            .writeValue(Paths.get(tracesPath).toFile(), traceResults)
        echo("[pipeline] Traces saved to: $tracesPath")
        writePluginArtifacts(rows)
        echo("[pipeline] Plugin artifacts saved to: .jbeval/eval (tools.json) and .jbeval/datasets (dataset.json)")

        echo("[pipeline] Converting traces to evaluation dataset…")
        echo("extractor output path: $extractorOutputPath")
        val prepared = prepareEval(tracesPath, extractorOutputPath, preparedName)
        echo("[pipeline] Dataset saved to: $prepared")

        echo("[pipeline] Loading evaluation config…")
        val configName = preparedName
        val evalRunConfig = loadConfig(configName)
        echo("[pipeline] Config loaded: ${evalRunConfig.name}")

        echo("[pipeline] Running evaluation…")
        runEvaluation(evalRunConfig, prepared)
        echo("[pipeline] Done.")
    }

    private fun runTracing(rows: List<Pair<String, String>>): List<DataPoint> {
        val pyExe = System.getenv("PYTHON")?.takeIf { it.isNotBlank() } ?: "python3"
        // TODO: (@gas) get the python files path from docker's env vars.
        val aiProfiler = Paths.get("/home/runner/app", "python", "ai_profiler.py").toString()
        val results = mutableListOf<DataPoint>()
        val expId = "trace_${timestamp()}"
        var idx = 1

        for ((input, expected) in rows) {
            val projectPath = Paths.get(projectDir)
            val runConfigName = evalRunConfig?.runConfigName?.takeIf { it.isNotBlank() } ?: "main"

            val runnerConfig = RunnerConfigRepository.loadRunnerConfig(projectPath, runConfigName)

            echo("runner config: ${gsonPretty.toJson(runnerConfig)}")

            val inputPath = runnerConfig?.mapping?.input?.trim()?.takeIf { it.isNotEmpty() }

            val packedInput = if (inputPath != null) {
                val inputStructJson = runnerConfig.inputStruct?.let {
                    GsonBuilder().setPrettyPrinting().create().toJson(it)
                }
                if (inputStructJson != null) {
                    InputStructurePacker.reconstructInput(inputPath, input, inputStructJson)
                } else {
                    input
                }
            } else {
                input
            }

            val id = idx.toString()
            idx++

            val tmpFile = Files.createTempFile("trace_", ".json")

            val cmd = mutableListOf(pyExe, aiProfiler, "--file", tmpFile.toString())
            cmd.add("--script"); cmd.add(script)
            cmd.add("--input"); cmd.add(packedInput)
            cmd.add(packedInput)
            cmd.add("--")
            val pb = ProcessBuilder(cmd).apply {
                directory(java.io.File(projectDir))
                redirectErrorStream(true)
                environment().apply {
                    val sep = java.io.File.pathSeparator
                    val base = java.io.File(projectDir).absolutePath
                    val existing = this["PYTHONPATH"]?.takeIf { it.isNotBlank() }
                    this["PYTHONPATH"] = if (existing == null) base else "$base$sep$existing"
                }
            }
            val p = pb.start()
            val combinedOut =
                runCatching { p.inputStream.bufferedReader().use { it.readText() } }.getOrNull()?.trim().orEmpty()
            val exit = p.waitFor()

            if (exit != 0) {
                val msg = "ai_profiler.py exited with code $exit"
                echo("[TRACE] id=$id: $msg")
                val rawErr = linkedMapOf<String, Any?>("error" to msg, "exitCode" to exit).apply {
                    if (combinedOut.isNotEmpty()) put("log", combinedOut)
                }
                results.add(
                    DataPoint(
                        id = id,
                        input = packedInput,
                        outputGen = "",
                        outputExpected = expected,
                        experimentId = expId,
                        raw = rawErr,
                        exception = msg
                    )
                )
                continue
            }

            val text = Files.readString(tmpFile).trim()
            val serializableTraceEventsFlat = parseFlatEventsJson(text)
            val hstate = buildHierarchicalStructureFromSerializableEvents(serializableTraceEventsFlat)
            val rawMap = serializeHierarchicalTraceEventsStateToMap(hstate)

            results.add(
                DataPoint(
                    id = id,
                    input = packedInput,
                    outputGen = "",
                    outputExpected = expected,
                    experimentId = expId,
                    raw = rawMap
                )
            )
            runCatching { Files.deleteIfExists(tmpFile) }
        }
        return results
    }

    private fun buildHierarchicalStructureFromSerializableEvents(serializableTraceEventsFlat: List<SerializableTraceEvent>): HierarchicalTraceEventsState {
        val sessionCounters = SessionCountersImpl()
        val eventsFlow = serializableTraceEventsFlat.asFlow()
        val repository = CommonTraceEventsRepository(
            scope,
            eventsFlow,
            sessionCounters,
        )

        runBlocking {
            // avoid infinite waits if repository never flips finished due to an exception
            withTimeoutOrNull(60_000) {
                repository.finished.first { it }
                if (serializableTraceEventsFlat.isNotEmpty()) {
                    repository.state.first { state ->
                        state.events.size == serializableTraceEventsFlat.size
                    }
                }
            }
        }

        val state = repository.state.value
        val hstate = buildHierarchicalStructure(state)
        return hstate
    }

    private fun parseFlatEventsJson(text: String): List<SerializableTraceEvent> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        @Suppress("UNCHECKED_CAST")
        try {
            val el = gson.fromJson(trimmed, JsonElement::class.java)
            when {
                el.isJsonArray -> {
                    val list = el.asJsonArray.mapNotNull {
                        CommonTraceEventsParser.parse(it.toString())
                    }
                    return list
                }
                else -> return emptyList()
            }
        } catch (_: Throwable) {
            val events = mutableListOf<SerializableTraceEvent>()
            trimmed.lines().forEach { line ->
                val s = line.trim()
                if (s.isNotEmpty()) {
                    runCatching {
                        val parsed = CommonTraceEventsParser.parse(s)
                        events.add(parsed)
                    }
                }
            }
            return events
        }
    }

    private fun prepareEval(traces: String, outputPath: String, preparedNameOpt: String?): String {
        val gson = Gson()
        val arr: List<Map<String, Any?>> =
            Files.newBufferedReader(Paths.get(traces)).use { r ->
                @Suppress("UNCHECKED_CAST")
                (gson.fromJson(r, List::class.java) as List<Map<String, Any?>>?) ?: emptyList()
            }

        val extractor = DebuggerTracesExtractor(outputPath)
        val now = java.time.LocalDateTime.now().toString()
        val expId = "exp_${timestamp()}"

        val dps = arr.mapIndexed { idx, e ->
            val id = (e["id"] as? String)?.ifBlank { (idx + 1).toString() } ?: (idx + 1).toString()
            val input = (e["input"] as? String)?.trim().orEmpty()
            val expected = (e["outputExpected"] as? String)?.trim()

            @Suppress("UNCHECKED_CAST")
            val raw0 = (e["raw"] as? Map<String, Any?>) ?: emptyMap<String, Any?>()
            val fallback = FallbackContext(
                id = id, input = input, outputExpected = expected, experimentId = expId
            )
            var dp = extractor.extract(raw0, fallback)
            dp = dp.copy(id = (idx + 1).toString(), runDatetime = now)
            dp
        }
        val outDir = Paths.get(".jbeval", "eval")
        try {
            Files.createDirectories(outDir)
        } catch (_: Throwable) {
        }
        val chosenName = run {
            val opt = preparedNameOpt?.trim().orEmpty()
            if (opt.isNotEmpty()) {
                val base = java.io.File(opt).name
                val sanitized = base.replace("\\", "_").replace("/", "_")
                if (sanitized.lowercase().endsWith(".json")) sanitized else "$sanitized.json"
            } else {
                "exp_${timestamp()}.json"
            }
        }
        val outPath = outDir.resolve(chosenName)
        Files.newBufferedWriter(outPath).use { w -> gsonPretty.toJson(dps, w) }
        return outPath.toString()
    }

    private fun loadConfig(configName: String): EvalRunConfig {
        val configPath = Paths.get(".jbeval", "remote", "next", configName, "$configName.json")

        if (!Files.exists(configPath)) {
            throw RuntimeException("Config file not found in staging area: $configPath")
        }

        return try {
            jacksonMapper.readValue(configPath.toFile(), EvalRunConfig::class.java)
        } catch (e: Exception) {
            throw RuntimeException("Failed to read config from $configPath", e)
        }
    }

    private fun runEvaluation(evalRunConfig: EvalRunConfig, datasetPath: String) {
        val dsPath = Paths.get(datasetPath)
        val data: List<DataPoint> = jacksonMapper.readValue(dsPath.toFile())
        require(data.isNotEmpty()) { "Dataset is empty: $dsPath" }

        val evalLlmProviderType = evalRunConfig.providerType ?: LlmProviderType.OPENAI
        val apiKey = UniversalApiKeyResolver.resolve(evalLlmProviderType, projectDir)
        val config = LlmProviderConfig(
            apiKey = apiKey,
            modelName = evalRunConfig.modelName ?: "",
            temperature = evalRunConfig.modelParams?.get("temperature")?.toDoubleOrNull() ?: 0.0,
            providerType = evalLlmProviderType
        )
        val provider = createLlmProvider<LLMScore>(config)

        val evaluatorConfigs = evalRunConfig.evaluators?.takeIf { it.isNotEmpty() }
            ?: listOf(
                com.intellij.aidebugger.evaluation.models.entities.EvaluatorConfig(
                    name = "Evaluator_1",
                    type = "llm judge",
                    prompt = evalRunConfig.promptTemplate
                )
            )

        echo("[pipeline] Creating ${evaluatorConfigs.size} evaluator(s): ${evaluatorConfigs.map { it.name to it.type }}")

        val evaluators = evaluatorConfigs.map { evalConfig ->
            val effectivePattern = when (evalConfig.type) {
                "regexp" -> evalConfig.pattern ?: ".*{outputExpected}.*"
                else -> null
            }
            val patternInfo = if (effectivePattern != null) " with pattern='$effectivePattern'" else ""
            echo("[pipeline] Creating evaluator '${evalConfig.name}' of type '${evalConfig.type}'$patternInfo")
            val inputVars = evalConfig.prompt?.let { extractInputVarsFromTemplate(it) } ?: emptyMap()
            EvaluatorEntry(
                name = evalConfig.name,
                type = evalConfig.type,
                instance = EvaluatorFactory.createFromConfig(evalConfig, provider, inputVars)
            )
        }

        echo("[pipeline] Running evaluation on ${data.size} data points with ${evaluators.size} evaluators...")

        val result = kotlinx.coroutines.runBlocking {
            EvaluationRunner.evaluateAndAggregate(
                dataPoints = data,
                evaluators = evaluators,
                progressCallback = { idx: Int, total: Int, dataPointId: String, evaluatorName: String ->
                    echo("[pipeline] Evaluating [$idx/$total] dataPoint=$dataPointId with evaluator=$evaluatorName")
                }
            )
        }

        val agg = result.first
        val allResults = result.second

        echo("[pipeline] Evaluation complete. Results by evaluator:")
        agg.evaluatorsStats.forEach { (evaluatorName: String, stats) ->
            echo("[pipeline]   $evaluatorName: mean=${String.format("%.3f", stats.mean)}, count=${stats.count}")
        }

        val sanitizedName = sanitizeNameForFile(evalRunConfig.name)
        val outDir = Paths.get(projectDir, ".jbeval", "eval", sanitizedName)
        val aggFile = saveEvaluationResults(agg, allResults, outDir)
        echo("[pipeline] Evaluation results written to: $aggFile")
    }

    private fun extractInputVarsFromTemplate(template: String): Map<String, String> {
        val varRegex = Regex("\\{([a-zA-Z0-9_]+)\\}")
        val vars = varRegex.findAll(template).map { it.groupValues[1] }.toSet()
        return vars.associateWith { it }
    }

    private fun writePluginArtifacts(rows: List<Pair<String, String>>) {
        val evalDir = Paths.get(".jbeval", "eval")
        val datasetsDir = Paths.get(".jbeval", "datasets")
        Files.createDirectories(evalDir)
        Files.createDirectories(datasetsDir)

        val items = JsonArray()
        var i = 1
        for ((inputStr, expectedStr) in rows) {
            val item = JsonObject()
            item.addProperty("id", "item_${i.toString().padStart(4, '0')}")
            item.add("input", toJsonElement(inputStr))
            item.add("output", toJsonElement(expectedStr))
            items.add(item)
            i++
        }

        val root = JsonObject()
        val meta = JsonObject()
        meta.addProperty("itemsCount", items.size())
        root.add("meta", meta)
        root.add("items", items)
        Files.writeString(datasetsDir.resolve("dataset.json"), root.toString())
    }

    private fun toJsonElement(value: Any?): JsonElement {
        if (value == null) return JsonNull.INSTANCE
        return when (value) {
            is JsonElement -> value
            is String -> parseStringAsJsonOrPrimitive(value)
            else -> gson.toJsonTree(value)
        }
    }

    private fun parseStringAsJsonOrPrimitive(text: String): JsonElement {
        val s = text.trim()
        return try {
            val looksJson = s.startsWith("{") || s.startsWith("[") || s.equals("null", true) ||
                    s.equals("true", true) || s.equals("false", true) ||
                    s.firstOrNull()?.isDigit() == true || (s.startsWith('"') && s.endsWith('"'))
            if (looksJson) JsonParser.parseString(s) else JsonPrimitive(text)
        } catch (_: Throwable) {
            JsonPrimitive(text)
        }
    }

    private fun readDataset(path: String): List<Pair<String, String>> {
        val p = Paths.get(path)
        val name = p.fileName.toString().lowercase()
        return when {
            name.endsWith(".csv") -> CsvDatasetReader.readCsv(p)
            name.endsWith(".json") -> JsonDatasetReader.readJson(p)
            else -> Files.readAllLines(p).mapNotNull { line ->
                val s = line.trim()
                if (s.isEmpty()) null else s to ""
            }
        }
    }

    private fun timestamp(): String =
        java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
}

private fun defaultOutDir(): String {
    return "/workspace/.jbeval/remote/out"
}

private fun detectPreparedConfigNameFromNext(): String? {
    return try {
        val base = Paths.get(".jbeval", "remote", "next")
        if (!Files.exists(base)) return null
        var dirName: String? = null
        Files.newDirectoryStream(base).use { ds ->
            for (p in ds) {
                if (!Files.isDirectory(p)) continue
                if (dirName != null) {
                    // More than one directory found; ambiguous -> fallback
                    return null
                }
                dirName = p.fileName.toString()
            }
        }
        dirName
    } catch (_: Throwable) {
        null
    }
}

fun main(args: Array<String>) {
    EvaluationCli()
        .subcommands(PipelineCommand())
        .versionOption(EvaluationCli.VERSION)
        .main(args)
}