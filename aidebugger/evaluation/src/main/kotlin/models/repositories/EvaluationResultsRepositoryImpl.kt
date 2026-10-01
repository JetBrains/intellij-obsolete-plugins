package com.intellij.aidebugger.evaluation.models.repositories

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.aidebugger.common.models.deserializeTraceEventsStateFromHierarchicalStateMap
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.PayloadKey
import com.intellij.aidebugger.evaluation.models.entities.AggregatedEvalResult
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.EvalResult
import com.intellij.aidebugger.evaluation.models.storage.sanitizeNameForFile
import com.intellij.aidebugger.evaluation.models.storage.saveEvaluationResults
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.max

@Service(Service.Level.PROJECT)
class EvaluationResultsRepositoryImpl(private val project: Project) : EvaluationResultsRepository {

    private val jsonMapper = jacksonObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun saveTableSnapshot(configName: String, tableSnapshot: TableSnapshot) {
        runCatching {
            val dir = evalDirRoot()
            Files.createDirectories(dir)
            val json = jsonMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(tableSnapshot)
            Files.writeString(lastResultsFileForConfig(configName), json)
        }.getOrElse { /* ignore */ }
    }

    override fun loadTableSnapshot(configName: String): TableSnapshot {
        return runCatching {
            val file = lastResultsFileForConfig(configName)
            if (!Files.exists(file)) {
                return TableSnapshot(emptyList(), emptyList(), emptyList())
            }
            jsonMapper.readValue(Files.readString(file), TableSnapshot::class.java)
        }.getOrElse { TableSnapshot(emptyList(), emptyList(), emptyList()) }
    }

    override fun saveEvaluationResult(
        aggregatedEvalResults: AggregatedEvalResult,
        evalResults: List<EvalResult>,
        outputDir: Path,
        selectedName: String?
    ): Path {
        return saveEvaluationResults(aggregatedEvalResults, evalResults, outputDir, selectedName)
    }

    override fun getAverageScore(configName: String): Double {
        val base = project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
        val dir = base.resolve(".jbeval").resolve("eval")
        val file = dir.resolve("last_${sanitizeNameForFile(configName)}.json")
        if (!Files.exists(file)) return Double.NaN
        val text = Files.readString(file)
        val mapper = ObjectMapper()
        val tree = mapper.readTree(text)
        val meanNode = tree.get("aggregated")?.get("stats")?.get("mean")
        if (meanNode != null && meanNode.isNumber) {
            val mean = meanNode.asDouble()
            return mean
        } else {
            val scoresNode = tree.get("scores")
            val scores = scoresNode?.map { it.asText() } ?: emptyList()
            if (scores.isEmpty()) return Double.NaN
            val nums = scores.map { it.trim().toDoubleOrNull() }
            if (nums.any { it == null }) return Double.NaN
            val avg = nums.filterNotNull().average()
            return avg
        }
    }

    override fun getAverageAgentTime(configName: String): Double {
        val dps = readDataPoints(project, configName)
        if (dps.isEmpty()) return Double.NaN
        val avgSec = dps.mapNotNull { dp ->
            runCatching {
                val state = deserializeTraceEventsStateFromHierarchicalStateMap(dp.raw)
                val events = state.events.values
                if (events.isEmpty()) null else {
                    val minStart = events.minOf { it.timestampStartMs }
                    val maxEnd = events.maxOf { it.timestampEndMs }
                    ((maxEnd - minStart).coerceAtLeast(0).toDouble() / 1000.0)
                }
            }.getOrNull()
        }.let { if (it.isEmpty()) Double.NaN else it.average() }
        return avgSec
    }

    override fun getAverageTokens(configName: String): Int {
        val dps = readDataPoints(project, configName)
        if (dps.isEmpty()) return 0
        val avgTokens = dps.mapNotNull { dp ->
            runCatching {
                val state = deserializeTraceEventsStateFromHierarchicalStateMap(dp.raw)
                state.events.values
                    .filter { it.type == EventType.LlmCall }
                    .mapNotNull { ev ->
                        extractTotalTokens(ev.payload[PayloadKey.Outputs])
                    }.sum().takeIf { it > 0.0 }
            }.getOrNull()
        }.let { if (it.isEmpty()) Double.NaN else it.average() }
        if (avgTokens.isNaN()) {
            return 0
        } else {
            return avgTokens.toInt()
        }
    }

    override fun getLastRunTimestamp(configName: String): Long? {
        val lastFile = lastResultsFileForConfig(configName)
        val dataFile = evalDirRoot().resolve("${sanitizeNameForFile(configName)}.json")

        val lastFileTime = if (Files.exists(lastFile)) {
            try {
                Files.getLastModifiedTime(lastFile).toMillis()
            } catch (e: Exception) {
                null
            }
        } else null

        val dataFileTime = if (Files.exists(dataFile)) {
            try {
                Files.getLastModifiedTime(dataFile).toMillis()
            } catch (e: Exception) {
                null
            }
        } else null

        return when {
            lastFileTime != null && dataFileTime != null -> max(lastFileTime, dataFileTime)
            lastFileTime != null -> lastFileTime
            dataFileTime != null -> dataFileTime
            else -> null
        }
    }

    override fun getLastAggregatedResult(configName: String): AggregatedEvalResult? {
        val file = lastResultsFileForConfig(configName)
        if (!Files.exists(file)) return null
        return try {
            val snapshot = jsonMapper.readValue(file.toFile(), TableSnapshot::class.java)
            if (snapshot.aggregatedStats.isNotEmpty()) {
                AggregatedEvalResult("latest", snapshot.aggregatedStats)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun removeResults(configName: String) {
        val sanitized = sanitizeNameForFile(configName)
        val root = evalDirRoot()
        try {
            Files.deleteIfExists(root.resolve("last_$sanitized.json"))
        } catch (e: Exception) {
            // ignore
        }
        try {
            Files.deleteIfExists(root.resolve("$sanitized.json"))
        } catch (e: Exception) {
            // ignore
        }
        try {
            val configDir = root.resolve(sanitized)
            if (Files.exists(configDir) && Files.isDirectory(configDir)) {
                Files.walk(configDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.delete(it) }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun readDataPoints(project: Project, configName: String): List<DataPoint> {
        val base = project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
        val dir = base.resolve(".jbeval").resolve("eval")
        val file = dir.resolve("${sanitizeNameForFile(configName)}.json")
        if (!Files.exists(file)) return emptyList()
        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        return Files.newBufferedReader(file).use { br ->
            mapper.readValue(br)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractTotalTokens(outputs: Any?): Double? {
        if (outputs == null) return null
        val last = getLastMessage(outputs) as? Map<*, *> ?: return null
        val responseMetadata = last["response_metadata"] as? Map<*, *> ?: return null
        val tokenUsage = responseMetadata["token_usage"] as? Map<*, *> ?: return null
        val totalTokens = tokenUsage["total_tokens"]
        return when (totalTokens) {
            is Number -> totalTokens.toDouble()
            is String -> totalTokens.toDoubleOrNull()
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getLastMessage(data: Any?): Any? = when (data) {
        is Map<*, *> -> {
            val mapData = data as? Map<String, Any?> ?: return null
            val messages = mapData.entries.firstOrNull { (k, v) ->
                k.equals("messages", ignoreCase = true) && v is List<*>
            }?.value as? List<*>
            messages?.lastOrNull()
        }
        is List<*> -> data.firstNotNullOfOrNull { getLastMessage(it) }
        else -> null
    }

    // Root directory for storing per-config last run results
    private fun evalDirRoot(): Path {
        val base = project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
        return base.resolve(".jbeval").resolve("eval")
    }

    private fun lastResultsFileForConfig(configName: String): Path {
        val sanitizedConfigName = sanitizeNameForFile(configName)
        return evalDirRoot().resolve("last_${sanitizedConfigName}.json")
    }
}
