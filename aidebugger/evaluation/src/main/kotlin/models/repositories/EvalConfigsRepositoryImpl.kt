package com.intellij.aidebugger.evaluation.models.repositories

import com.google.gson.Gson
import com.intellij.aidebugger.common.models.TracesDatasetsRepository
import com.intellij.aidebugger.evaluation.models.entities.ConfigInfo
import com.intellij.aidebugger.evaluation.models.entities.EvalRunConfig
import com.intellij.aidebugger.evaluation.models.storage.sanitizeNameForFile
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.notExists

@Service(Service.Level.PROJECT)
class EvalConfigsRepositoryImpl(private val project: Project) : EvalConfigsRepository {

    private data class MetadataEntry(
        var name: String,
        val fileName: String
    )

    private data class Metadata(val configs: MutableList<MetadataEntry> = mutableListOf())

    private val gson = Gson()

    private fun ensureConfigsDir(): Path {
        val base = project.basePath ?: System.getProperty("user.dir")
        val dir = Paths.get(base).resolve(".jbeval").resolve("configs")
        if (dir.notExists()) Files.createDirectories(dir)
        return dir
    }

    private fun metadataPath(): Path = ensureConfigsDir().resolve(".metadata")

    private fun writeMetadata(md: Metadata) {
        try {
            Files.newBufferedWriter(metadataPath(), StandardCharsets.UTF_8).use { w ->
                gson.toJson(md, w)
            }
        } catch (_: Throwable) { }
    }

    private fun buildFallbackEntries(): MutableList<MetadataEntry> {
        val dir = ensureConfigsDir()
        val list = mutableListOf<MetadataEntry>()
        try {
            Files.list(dir).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .forEach { p ->
                        val fileName = p.fileName.toString()
                        if (!fileName.startsWith(".") && fileName.endsWith(".json", ignoreCase = true)) {
                            val name = fileName.removeSuffix(".json")
                            list.add(MetadataEntry(name, fileName))
                        }
                    }
            }
        } catch (_: Throwable) { }
        return list
    }

    private fun readMetadata(): Metadata {
        val path = metadataPath()
        if (path.notExists()) return Metadata(buildFallbackEntries())
        return Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
            gson.fromJson(reader, Metadata::class.java) ?: Metadata(buildFallbackEntries())
        }
    }

    override fun listConfigs(): List<ConfigInfo> {
        val md = readMetadata()
        val dir = ensureConfigsDir()
        val existing = md.configs.filter {
            val fn = it.fileName
            fn.endsWith(".json", ignoreCase = true) && !fn.startsWith(".") && Files.exists(dir.resolve(fn))
        }
        val unique = LinkedHashMap<String, MetadataEntry>()
        for (e in existing) unique.putIfAbsent(e.fileName, e)
        return unique.values.map { ConfigInfo(it.name, it.fileName) }
    }

    override fun removeConfig(name: String) {
        val md = readMetadata()
        val it = md.configs.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (e.name == name) {
                it.remove()
                val dir = ensureConfigsDir()
                Files.deleteIfExists(dir.resolve(e.fileName))
            }
        }
        writeMetadata(md)
    }

    override fun renameConfig(oldName: String, newName: String): ConfigInfo? {
        val md = readMetadata()
        var found: MetadataEntry? = null
        for (e in md.configs) {
            if (e.name == oldName) { found = e; break }
        }
        if (found == null) return null
        found.name = newName
        writeMetadata(md)
        return ConfigInfo(found.name, found.fileName)
    }

    override fun createOrUpdateConfig(name: String, cfg: EvalRunConfig): ConfigInfo {
        val dir = ensureConfigsDir()
        val md = readMetadata()

        val sanitized = sanitizeNameForFile(name.ifBlank { cfg.name.ifBlank { "config" } }).ifBlank { "config" }
        val existingEntry = md.configs.firstOrNull { it.name.equals(sanitized, ignoreCase = true) }
        val fileName = existingEntry?.fileName ?: uniqueJsonFileName(dir, sanitized)

        val info = ConfigInfo(sanitized, fileName)
        cfg.name = sanitized
        normalizeOrInferDatasetName(cfg.datasetName)?.let { cfg.datasetName = it }
        writeJson(dir.resolve(fileName), cfg)

        if (existingEntry == null) {
            md.configs.add(MetadataEntry(sanitized, fileName))
            writeMetadata(md)
        }

        return info
    }

    override fun loadConfig(info: ConfigInfo): EvalRunConfig {
        val dir = ensureConfigsDir()
        return loadConfigFromPath(dir.resolve(info.fileName))
    }

    private fun loadConfigFromPath(path: Path): EvalRunConfig {
        return try {
            val cfg = Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
                gson.fromJson(reader, EvalRunConfig::class.java) ?: EvalRunConfig()
            }
            val normalized = normalizeOrInferDatasetName(cfg.datasetName)
            if (normalized != null && normalized != cfg.datasetName) {
                cfg.datasetName = normalized
                runCatching { writeJson(path, cfg) }
            }
            cfg
        } catch (_: Throwable) {
            EvalRunConfig()
        }
    }

    override fun getConfigPathByName(name: String): Path? {
        val md = readMetadata()
        val entry = md.configs.firstOrNull { it.name == name }
            ?: return null
        return ensureConfigsDir().resolve(entry.fileName)
    }

    private fun writeJson(path: Path, cfg: EvalRunConfig) {
        try {
            Files.newBufferedWriter(path, StandardCharsets.UTF_8).use { w -> gson.toJson(cfg, w) }
        } catch (_: Throwable) { }
    }

    private fun normalizeOrInferDatasetName(ref: String?): String? {
        val repo = try { project.service<TracesDatasetsRepository>() } catch (_: Throwable) { null }
        val all = try { repo?.listDatasets() ?: emptyList() } catch (_: Throwable) { emptyList() }
        if (ref.isNullOrBlank()) {
            return if (all.size == 1) all.first().name else null
        }
        all.firstOrNull { it.name == ref }?.let { return it.name }
        val fileName = runCatching { Paths.get(ref).fileName?.toString() }.getOrNull()
        if (!fileName.isNullOrBlank()) {
            all.firstOrNull { it.fileName == fileName }?.let { return it.name }
        }
        return null
    }

    private fun uniqueJsonFileName(dir: Path, candidate: String): String {
        val candidateFileName = "$candidate.json"
        if (Files.notExists(dir.resolve(candidateFileName))) return candidateFileName
        val base = candidate.removeSuffix(".json")
        var i = 1
        while (true) {
            val alt = "$base.$i.json"
            if (Files.notExists(dir.resolve(alt))) return alt
            i++
        }
    }
}
