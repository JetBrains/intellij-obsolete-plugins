package com.intellij.aidebugger.evaluation.intellij

import com.intellij.openapi.project.Project
import java.io.File

/**
 * Aggregates environment variables for the Python process from multiple sources
 * in the following order (last wins on key conflicts):
 * 1) Explicit Run Configuration (by path/name if provided)
 * 2) Active Run Configuration (if available)
 * 3) .env file located in the project root (if present)
 */
object EnvVarsAggregator {
    data class RunEnvConfig(val vars: Map<String, String>, val passParentEnvs: Boolean)

    fun aggregate(project: Project, rcName: String): RunEnvConfig? {
        val projectDir = project.basePath ?: ""
        val merged = LinkedHashMap<String, String>()
        var lastPassParent: Boolean? = null

        runCatching { RunConfigResolver.resolveEnvVarsFromRunConfig(project, rcName) }.getOrNull()?.let { cfg ->
            merged.putAll(cfg.vars)
            lastPassParent = cfg.passParentEnvs
        }
        val dotenv = readDotEnv(projectDir)
        if (dotenv.isNotEmpty()) {
            merged.putAll(dotenv)
        }

        return if (merged.isEmpty() && lastPassParent == null) null else RunEnvConfig(merged, lastPassParent ?: true)
    }

    fun readDotEnv(projectDir: String): Map<String, String> {
        val vars = LinkedHashMap<String, String>()
        val file = File(projectDir, ".env")
        if (!file.exists() || !file.isFile) return emptyMap()
        file.readLines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            if (line.startsWith("#")) return@forEach
            val withoutExport = if (line.startsWith("export ")) line.removePrefix("export ").trim() else line
            val idx = withoutExport.indexOf('=')
            if (idx <= 0) return@forEach
            val key = withoutExport.take(idx).trim()
            var value = withoutExport.substring(idx + 1)
            if (key.isEmpty()) return@forEach
            // Strip surrounding quotes if present
            if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith('\'') && value.endsWith('\''))) {
                value = value.substring(1, value.length - 1)
            }
            vars[key] = value
        }
        return vars
    }
}