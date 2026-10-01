package com.intellij.aidebugger.evaluation.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.repositories.EvaluationResultsRepositoryImpl
import com.intellij.aidebugger.evaluation.models.repositories.TableSnapshot
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class EvaluationPersistenceService(private val project: Project) {
    private val resultsRepository = project.service<EvaluationResultsRepositoryImpl>()
    private val dataPointsCache = ConcurrentHashMap<String, List<DataPoint>>()
    private val LOG = Logger.getInstance("EvaluationPersistenceService")

    private val baseOut by lazy {
        project.basePath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.home"))
    }

    fun loadTableSnapshot(configName: String): TableSnapshot {
        return resultsRepository.loadTableSnapshot(configName)
    }

    fun saveTableSnapshot(configName: String, snapshot: TableSnapshot) {
        resultsRepository.saveTableSnapshot(configName, snapshot)
    }

    fun getOrLoadDataPoints(configName: String): List<DataPoint>? {
        dataPointsCache[configName]?.let { return it }

        val sanitized = sanitizeConfigName(configName)
        val file = baseOut.resolve(".jbeval").resolve("eval").resolve("${sanitized}.json")

        if (Files.exists(file)) {
            try {
                val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
                val type = mapper.typeFactory.constructCollectionType(
                    java.util.List::class.java,
                    DataPoint::class.java
                )
                Files.newBufferedReader(file).use { br ->
                    val dps: List<DataPoint> = mapper.readValue(br, type)
                    dataPointsCache[configName] = dps
                    return dps
                }
            } catch (e: Exception) {
                LOG.warn("Failed to load datapoints for $configName", e)
            }
        }
        return null
    }

    fun invalidateCache(configName: String? = null) {
        if (configName != null) dataPointsCache.remove(configName) else dataPointsCache.clear()
    }

    private fun sanitizeConfigName(configName: String): String =
        configName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
}