package com.intellij.aidebugger.common.models

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.intellij.aidebugger.common.utility.FileNameSanitizer
import com.intellij.aidebugger.common.utility.GsonUtil
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

data class RunnerConfig(
    val runConfig: String,
    val mapping: MappingConfig,
    val inputStruct: JsonElement? = null
)

data class MappingConfig(
    val input: String,
    val output: String
)

object RunnerConfigRepository {
    val gson = GsonUtil.prettyGson

    fun loadRunnerConfig(basePath: Path, runConfigName: String): RunnerConfig? {
        return runCatching {
            val file = getConfigFile(basePath, runConfigName)
            if (Files.exists(file)) {
                Files.newBufferedReader(file).use { reader ->
                    gson.fromJson(reader, RunnerConfig::class.java)
                }
            } else null
        }.getOrNull()
    }

    fun saveRunnerConfig(
        basePath: Path,
        runConfigName: String,
        inputPath: String,
        outputPath: String,
        inputStruct: Any? = null
    ): Boolean {
        return runCatching {
            val file = getConfigFile(basePath, runConfigName)
            Files.createDirectories(file.parent)

            val structElement: JsonElement? = when (inputStruct) {
                null -> null
                is String -> {
                    try {
                        JsonParser.parseString(inputStruct)
                    } catch (e: Exception) {
                        GsonUtil.toJsonElement(inputStruct)
                    }
                }
                is JsonElement -> inputStruct
                else -> GsonUtil.toJsonElement(inputStruct)
            }

            val config = RunnerConfig(
                runConfig = runConfigName,
                mapping = MappingConfig(
                    input = inputPath,
                    output = outputPath
                ),
                inputStruct = structElement
            )

            Files.newBufferedWriter(file).use { writer ->
                gson.toJson(config, writer)
            }
            true
        }.getOrElse { e ->
            println("Failed to save runner config: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun getConfigFile(basePath: Path, runConfigName: String): Path {
        val sanitized = runConfigName.trim()
            .ifEmpty { UUID.randomUUID().toString() }
            .let { FileNameSanitizer.sanitize(it) }
        return basePath.resolve(".jbeval/runners/$sanitized.json")
    }
}