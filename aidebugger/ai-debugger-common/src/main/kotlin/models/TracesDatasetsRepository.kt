package com.intellij.aidebugger.common.models

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.intellij.aidebugger.common.utility.FileNameSanitizer
import com.intellij.aidebugger.common.utility.GsonUtil
import com.intellij.aidebugger.common.utility.JsonPathUtils
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import kotlin.io.path.notExists

data class Metadata(val datasets: MutableList<DatasetInfo> = mutableListOf())

data class TracesDatasetFile(
    var meta: JsonElement?,
    var items: MutableList<TraceDatasetEntry> = mutableListOf(),
    var data: MutableList<TraceDatasetEntry>? = null,
    var lastModified: Long = 0
)

data class TraceDatasetEntry(
    val id: Int,
    val input: JsonElement?,
    val output: JsonElement?,
    val raw: JsonElement?
)

data class DatasetRow(
    val input: String?,
    val output: String?,
    val raw: JsonElement?
)

@Service(Service.Level.PROJECT)
class TracesDatasetsRepository(private val project: Project) {
    private val gson: Gson = GsonUtil.gson

    private val _datasets = MutableStateFlow<List<DatasetInfo>>(emptyList())
    val datasets: StateFlow<List<DatasetInfo>> = _datasets.asStateFlow()

    private val _datasetContentChanged = MutableSharedFlow<String?>(replay = 0)
    val datasetContentChanged: SharedFlow<String?> = _datasetContentChanged.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        syncMetadataFromDatasetFiles()
        _datasets.value = listDatasetsInternal()
    }

    fun listDatasets(): List<DatasetInfo> {
        return _datasets.value
    }

    fun resolveDataset(datasetName: String?): DatasetInfo? {
        if (datasetName.isNullOrBlank()) return null
        val all = _datasets.value
        val byName = all.firstOrNull { it.name == datasetName }
        if (byName != null) return byName
        return try {
            val base = baseDir()
            all.firstOrNull { base.resolve(it.fileName).toString() == datasetName }
        } catch (_: Throwable) { null }
    }

    fun countRowsForDataset(datasetName: String?): Int {
        if (datasetName.isNullOrBlank()) return 0
        val ds = resolveDataset(datasetName) ?: return 0
        return ds.itemsCount
    }

    fun createDataset(name: String): DatasetInfo {
        val md = readMetadata()
        val sanitized = sanitizeName(name)
        val existing = md.datasets.find { it.name == sanitized }
        if (existing != null) return existing

        val dir = ensureBaseDir()
        val fileName = uniqueJsonFileName(dir, "$sanitized.json")
        val timestamp = System.currentTimeMillis()
        val entry = DatasetInfo(sanitized, fileName, itemsCount = 0, lastModified = timestamp)
        md.datasets.add(entry)
        writeMetadata(md)

        val datasetFile = TracesDatasetFile(
            meta = JsonObject(),
            items = mutableListOf(),
            data = null,
            lastModified = timestamp
        )
        val path = dir.resolve(fileName)
        try {
            Files.newBufferedWriter(path, StandardCharsets.UTF_8).use { w -> gson.toJson(datasetFile, w) }
        } catch (_: Throwable) { }

        refresh()
        kotlinx.coroutines.runBlocking { _datasetContentChanged.emit(entry.name) }
        return entry
    }

    fun deleteDataset(name: String): Boolean {
        val md = readMetadata()
        val idx = md.datasets.indexOfFirst { it.name == name }
        if (idx < 0) return false
        val file = ensureBaseDir().resolve(md.datasets[idx].fileName)
        val removed = try { Files.deleteIfExists(file) } catch (_: Throwable) { false }
        md.datasets.removeAt(idx)
        writeMetadata(md)
        if (removed) refresh()
        return removed
    }

    fun renameDataset(oldName: String, newName: String): DatasetInfo? {
        return try {
            val md = readMetadata()
            val entry = md.datasets.find { it.name == oldName } ?: return null
            val sanitized = sanitizeName(newName)
            entry.name = sanitized
            writeMetadata(md)
            val result = DatasetInfo(entry.name, entry.fileName)
            refresh()
            result
        } catch (_: Throwable) {
            null
        }
    }

    fun loadDataset(
        dataset: DatasetInfo,
        inputJsonPath: String? = null,
        outputJsonPath: String? = null
    ): List<DatasetRow> {
        val fileModel = readTracesDatasetFile(dataset) ?: return emptyList()

        val inputPathTokens = inputJsonPath?.let { JsonPathUtils.parseJsonPath(it) }
        val outputPathTokens = outputJsonPath?.let { JsonPathUtils.parseJsonPath(it) }

        return fileModel.items.map { item ->
            val inputString = resolveFieldAsString(
                direct = item.input,
                rawRoot = item.raw,
                pathTokens = inputPathTokens
            )
            val outputString = resolveFieldAsString(
                direct = item.output,
                rawRoot = item.raw,
                pathTokens = outputPathTokens
            )
            DatasetRow(
                input = inputString,
                output = outputString,
                raw = item.raw
            )
        }
    }

    fun saveDataset(dataset: DatasetInfo, rows: List<DatasetRow>): Boolean {
        val path = ensureBaseDir().resolve(dataset.fileName)
        return try {
            val fileModel = if (Files.exists(path)) {
                Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
                    gson.fromJson(reader, TracesDatasetFile::class.java)
                        ?: TracesDatasetFile(JsonObject(), mutableListOf(), null)
                }
            } else {
                TracesDatasetFile(JsonObject(), mutableListOf(), null)
            }

            fileModel.items.clear()

            var idx = 1
            for (row in rows) {
                fileModel.items.add(
                    TraceDatasetEntry(
                        id = idx,
                        input = toJsonElement(row.input),
                        output = toJsonElement(row.output),
                        raw = row.raw ?: JsonNull.INSTANCE
                    )
                )
                idx++
            }

            Files.newBufferedWriter(path, StandardCharsets.UTF_8).use { writer ->
                gson.toJson(fileModel, writer)
            }

            val md = readMetadata()
            val entry = md.datasets.find { it.fileName == dataset.fileName }
            if (entry != null) {
                entry.itemsCount = fileModel.items.size
                writeMetadata(md)
            }

            kotlinx.coroutines.runBlocking { _datasetContentChanged.emit(dataset.name) }
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun addEntry(datasetName: String, input: Any?, output: Any?, raw: Any?): Boolean {
        val md = readMetadata()
        val entry = md.datasets.find { it.name == sanitizeName(datasetName) } ?: return false
        val dataset = DatasetInfo(entry.name, entry.fileName, entry.itemsCount)

        val dir = ensureBaseDir()
        val path = dir.resolve(dataset.fileName)
        if (!Files.exists(path)) return false

        try {
            val fileContent = Files.readString(path, StandardCharsets.UTF_8)
            val datasetFile = gson.fromJson(fileContent, TracesDatasetFile::class.java)

            val items = datasetFile.items
            val newId = items.maxOfOrNull { it.id }?.plus(1) ?: 1
            val newEntry = TraceDatasetEntry(
                id = newId,
                input = toJsonElement(input),
                output = toJsonElement(output),
                raw = toJsonElement(raw)
            )
            items.add(newEntry)

            Files.newBufferedWriter(path, StandardCharsets.UTF_8).use { w ->
                gson.toJson(datasetFile, w)
            }

            entry.itemsCount = items.size
            writeMetadata(md)

            refresh()
            kotlinx.coroutines.runBlocking { _datasetContentChanged.emit(datasetName) }
            return true
        } catch (_: Throwable) {
            return false
        }
    }

    private fun readTracesDatasetFile(dataset: DatasetInfo): TracesDatasetFile? {
        val path = ensureBaseDir().resolve(dataset.fileName)
        return try {
            Files.newBufferedReader(path, StandardCharsets.UTF_8).use { r ->
                val fileModel = gson.fromJson(r, TracesDatasetFile::class.java) ?: return null
                if (fileModel.items.isEmpty() && fileModel.data != null && fileModel.data!!.isNotEmpty()) {
                    fileModel.items.addAll(fileModel.data!!)
                    fileModel.data = null
                }
                fileModel
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun resolveFieldAsString(
        direct: JsonElement?,
        rawRoot: JsonElement?,
        pathTokens: List<com.intellij.aidebugger.common.utility.JsonPathToken>?
    ): String? {
        JsonPathUtils.jsonStringLike(direct)?.let { return it }
        if (rawRoot == null || pathTokens == null) return null
        val byPath = JsonPathUtils.getValueByPath(rawRoot, pathTokens)
        return JsonPathUtils.jsonStringLike(byPath)
    }

    fun export(dataset: DatasetInfo, destination: Path): Boolean {
        return try {
            val src = ensureBaseDir().resolve(dataset.fileName)
            Files.createDirectories(destination.parent)
            Files.copy(src, destination, StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun baseDir(): Path {
        val base = project.basePath ?: System.getProperty("user.dir")
        return Paths.get(base).resolve(".jbeval").resolve("datasets")
    }

    private fun ensureBaseDir(): Path {
        val dir = baseDir()
        if (dir.notExists()) Files.createDirectories(dir)
        return dir
    }

    private fun metadataPath(): Path = ensureBaseDir().resolve(".metadata")

    private fun readMetadata(): Metadata {
        val path = metadataPath()
        return try {
            if (path.notExists()) Metadata(buildFallbackEntries()) else Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
                gson.fromJson(reader, Metadata::class.java) ?: Metadata(buildFallbackEntries())
            }
        } catch (_: Throwable) {
            Metadata(buildFallbackEntries())
        }
    }

    private fun writeMetadata(md: Metadata) {
        try {
            Files.newBufferedWriter(metadataPath(), StandardCharsets.UTF_8).use { w ->
                gson.toJson(md, w)
            }
        } catch (_: Throwable) { }
    }

    private fun buildFallbackEntries(): MutableList<DatasetInfo> {
        val dir = ensureBaseDir()
        val list = mutableListOf<DatasetInfo>()
        try {
            Files.list(dir).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .forEach { p ->
                        val fileName = p.fileName.toString()
                        if (!fileName.startsWith('.') && fileName.endsWith(".json", ignoreCase = true)) {
                            val name = fileName.removeSuffix(".json")
                            list.add(DatasetInfo(name, fileName))
                        }
                    }
            }
        } catch (_: Throwable) { }
        return list
    }

    private fun listDatasetsInternal(): List<DatasetInfo> {
        val md = readMetadata()
        val dir = ensureBaseDir()

        return md.datasets
            .filter { !it.fileName.startsWith(".") && Files.exists(dir.resolve(it.fileName)) }
            .map { entry ->
                // If timestamp is missing, try to recover it from file attributes
                if (entry.lastModified <= 0) {
                    val path = dir.resolve(entry.fileName)
                    entry.lastModified = try {
                        val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
                        val created = attrs.creationTime().toMillis()
                        val modified = attrs.lastModifiedTime().toMillis()
                        maxOf(created, modified)
                    } catch (_: Throwable) {
                        try { Files.getLastModifiedTime(path).toMillis() } catch (_: Throwable) { 0L }
                    }
                }
                entry
            }
            .sortedByDescending { it.lastModified }
    }

    private fun syncMetadataFromDatasetFiles() {
        val md = readMetadata()
        var changed = false

        for (entry in md.datasets) {
            val path = ensureBaseDir().resolve(entry.fileName)
            if (Files.exists(path)) {
                try {
                    Files.newBufferedReader(path, StandardCharsets.UTF_8).use { r ->
                        val fileModel = gson.fromJson(r, TracesDatasetFile::class.java)
                        if (fileModel != null) {
                            val itemsCount = fileModel.items.size
                            if (entry.itemsCount != itemsCount) {
                                entry.itemsCount = itemsCount
                                changed = true
                            }
                        }
                    }
                } catch (_: Throwable) { }
            }
        }

        if (changed) {
            writeMetadata(md)
        }
    }

    private fun sanitizeName(name: String): String {
        val n = name.trim().ifEmpty { UUID.randomUUID().toString() }
        return FileNameSanitizer.sanitize(n)
    }

    private fun uniqueJsonFileName(dir: Path, candidate: String): String {
        var name = candidate
        var idx = 1
        while (Files.exists(dir.resolve(name))) {
            val base = candidate.removeSuffix(".json")
            name = "$base($idx).json"
            idx++
        }
        return name
    }

    private fun toJsonElement(value: Any?): JsonElement? {
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
            val looksJson = s.startsWith("{") || s.startsWith("[") || s.equals("null", true) || s.equals("true", true) || s.equals("false", true) || s.firstOrNull()?.isDigit() == true || (s.startsWith('"') && s.endsWith('"'))
            if (looksJson) JsonParser.parseString(s) else JsonPrimitive(text)
        } catch (_: Throwable) {
            JsonPrimitive(text)
        }
    }
}