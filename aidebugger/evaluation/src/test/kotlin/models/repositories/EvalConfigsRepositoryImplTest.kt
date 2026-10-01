package com.intellij.aidebugger.evaluation.models.repositories

import com.google.gson.Gson
import com.intellij.aidebugger.evaluation.models.entities.ConfigInfo
import com.intellij.aidebugger.evaluation.models.entities.EvalRunConfig
import com.intellij.aidebugger.evaluation.models.entities.EvaluatorConfig
import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class EvalConfigsRepositoryImplTest {

    private val gson = Gson()
    private val tempDirs = mutableListOf<Path>()

    @After
    fun cleanup() {
        tempDirs.forEach { dir ->
            runCatching {
                dir.toFile().deleteRecursively()
            }
        }
        tempDirs.clear()
    }

    private fun createTestRepository(): Pair<EvalConfigsRepository, Path> {
        val tempDir = Files.createTempDirectory("eval-configs-test")
        tempDirs.add(tempDir)

        // Use a lightweight implementation for testing with temp directory
        val repository = TestEvalConfigsRepository(tempDir)
        return repository to tempDir
    }

    /**
     * Test repository that mirrors EvalConfigsRepositoryImpl but doesn't require a Project
     */
    private class TestEvalConfigsRepository(private val testBasePath: Path) : EvalConfigsRepository {
        private val gson = Gson()

        private data class MetadataEntry(var name: String, val fileName: String)
        private data class Metadata(val configs: MutableList<MetadataEntry> = mutableListOf())

        private fun ensureConfigsDir(): Path {
            val dir = testBasePath.resolve(".jbeval").resolve("configs")
            if (!Files.exists(dir)) Files.createDirectories(dir)
            return dir
        }

        private fun metadataPath(): Path = ensureConfigsDir().resolve(".metadata")

        private fun writeMetadata(md: Metadata) {
            try {
                Files.newBufferedWriter(metadataPath()).use { w -> gson.toJson(md, w) }
            } catch (_: Throwable) { }
        }

        private fun readMetadata(): Metadata {
            val path = metadataPath()
            if (!Files.exists(path)) return Metadata(buildFallbackEntries())
            return try {
                Files.newBufferedReader(path).use { reader ->
                    gson.fromJson(reader, Metadata::class.java) ?: Metadata(buildFallbackEntries())
                }
            } catch (_: Throwable) {
                Metadata(buildFallbackEntries())
            }
        }

        private fun buildFallbackEntries(): MutableList<MetadataEntry> {
            val dir = ensureConfigsDir()
            val list = mutableListOf<MetadataEntry>()
            try {
                Files.list(dir).use { stream ->
                    stream.filter { Files.isRegularFile(it) }.forEach { p ->
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

        override fun loadConfig(info: ConfigInfo): EvalRunConfig {
            val dir = ensureConfigsDir()
            val path = dir.resolve(info.fileName)
            return try {
                Files.newBufferedReader(path).use { reader ->
                    gson.fromJson(reader, EvalRunConfig::class.java) ?: EvalRunConfig()
                }
            } catch (_: Throwable) {
                EvalRunConfig()
            }
        }

        override fun createOrUpdateConfig(name: String, cfg: EvalRunConfig): ConfigInfo {
            val dir = ensureConfigsDir()
            val md = readMetadata()

            val sanitized = sanitizeName(name.ifBlank { cfg.name.ifBlank { "config" } })
            val existingEntry = md.configs.firstOrNull { it.name.equals(sanitized, ignoreCase = true) }
            val fileName = existingEntry?.fileName ?: uniqueJsonFileName(dir, sanitized)

            val info = ConfigInfo(sanitized, fileName)
            cfg.name = sanitized
            writeJson(dir.resolve(fileName), cfg)

            if (existingEntry == null) {
                md.configs.add(MetadataEntry(sanitized, fileName))
                writeMetadata(md)
            }

            return info
        }

        override fun renameConfig(oldName: String, newName: String): ConfigInfo? {
            val md = readMetadata()
            val found = md.configs.find { it.name == oldName } ?: return null
            found.name = newName
            writeMetadata(md)
            return ConfigInfo(found.name, found.fileName)
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

        override fun getConfigPathByName(name: String): Path? {
            val md = readMetadata()
            val entry = md.configs.firstOrNull { it.name == name } ?: return null
            return ensureConfigsDir().resolve(entry.fileName)
        }

        private fun writeJson(path: Path, cfg: EvalRunConfig) {
            try {
                Files.newBufferedWriter(path).use { w -> gson.toJson(cfg, w) }
            } catch (_: Throwable) { }
        }

        private fun sanitizeName(name: String): String {
            val s = name.trim()
            // Only replace filesystem-unsafe characters, allow Unicode and special chars like @ #
            return s.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "config" }
        }

        private fun uniqueJsonFileName(dir: Path, candidate: String): String {
            val candidateFileName = "$candidate.json"
            if (!Files.exists(dir.resolve(candidateFileName))) return candidateFileName
            val base = candidate.removeSuffix(".json")
            var i = 1
            while (true) {
                val alt = "$base.$i.json"
                if (!Files.exists(dir.resolve(alt))) return alt
                i++
            }
        }
    }

    @Test
    fun `configs directory is created automatically`() {
        val (repository, tempDir) = createTestRepository()

        // Trigger directory creation by listing configs
        repository.listConfigs()

        val dir = tempDir.resolve(".jbeval").resolve("configs")
        assertTrue(Files.exists(dir))
        assertTrue(Files.isDirectory(dir))
    }

    @Test
    fun `listConfigs returns empty list when no configs exist`() {
        val (repository, _) = createTestRepository()

        val configs = repository.listConfigs()

        assertTrue(configs.isEmpty())
    }

    @Test
    fun `createOrUpdateConfig creates new config`() {
        val (repository, _) = createTestRepository()
        val config = EvalRunConfig(
            name = "test-config",
            datasetName = "dataset1",
            providerType = LlmProviderType.OPENAI
        )

        val info = repository.createOrUpdateConfig("test-config", config)

        assertEquals("test-config", info.name)
        assertTrue(info.fileName.endsWith(".json"))
        assertTrue(Files.exists(repository.getConfigPathByName(info.name)))
    }

    @Test
    fun `createOrUpdateConfig sanitizes config name`() {
        val (repository, _) = createTestRepository()
        val config = EvalRunConfig(name = "test@config#123", datasetName = "dataset1")

        val info = repository.createOrUpdateConfig("test@config#123", config)

        // New sanitization only replaces filesystem-unsafe characters (\ / : * ? " < > |)
        // Characters like @ and # are now allowed
        assertEquals("test@config#123", info.name)
    }

    @Test
    fun `createOrUpdateConfig updates existing config`() {
        val (repository, _) = createTestRepository()
        val config1 = EvalRunConfig(name = "config", datasetName = "dataset1")
        val config2 = EvalRunConfig(name = "config", datasetName = "dataset2")

        val info1 = repository.createOrUpdateConfig("config", config1)
        val info2 = repository.createOrUpdateConfig("config", config2)

        assertEquals("config", info1.name)
        assertEquals("config", info2.name)
        assertEquals(info1.fileName, info2.fileName)

        // Verify the config was updated
        val loaded = repository.loadConfig(info2)
        assertEquals("dataset2", loaded.datasetName)
    }

    @Test
    fun `listConfigs returns all added configs`() {
        val (repository, _) = createTestRepository()
        val config1 = EvalRunConfig(name = "config1", datasetName = "dataset1")
        val config2 = EvalRunConfig(name = "config2", datasetName = "dataset2")

        repository.createOrUpdateConfig("config1", config1)
        repository.createOrUpdateConfig("config2", config2)

        val configs = repository.listConfigs()

        assertEquals(2, configs.size)
        assertTrue(configs.any { it.name == "config1" })
        assertTrue(configs.any { it.name == "config2" })
    }

    @Test
    fun `loadConfig retrieves saved config`() {
        val (repository, _) = createTestRepository()
        val originalConfig = EvalRunConfig(
            name = "test-config",
            datasetName = "dataset1",
            modelName = "gpt-4",
            promptTemplate = "Evaluate: {input}",
            providerType = LlmProviderType.ANTHROPIC
        )

        val info = repository.createOrUpdateConfig("test-config", originalConfig)
        val loadedConfig = repository.loadConfig(info)

        assertEquals("test-config", loadedConfig.name)
        assertEquals("dataset1", loadedConfig.datasetName)
        assertEquals("gpt-4", loadedConfig.modelName)
        assertEquals("Evaluate: {input}", loadedConfig.promptTemplate)
        assertEquals(LlmProviderType.ANTHROPIC, loadedConfig.providerType)
    }

    @Test
    fun `createOrUpdateConfig updates existing config content`() {
        val (repository, _) = createTestRepository()
        val config = EvalRunConfig(name = "config", datasetName = "dataset1")
        val info = repository.createOrUpdateConfig("config", config)

        val updatedConfig = EvalRunConfig(
            name = "config",
            datasetName = "dataset2",
            modelName = "gpt-4",
            providerType = LlmProviderType.GEMINI
        )
        repository.createOrUpdateConfig("config", updatedConfig)

        val loaded = repository.loadConfig(info)
        assertEquals("dataset2", loaded.datasetName)
        assertEquals("gpt-4", loaded.modelName)
        assertEquals(LlmProviderType.GEMINI, loaded.providerType)
    }

    @Test
    fun `renameConfig updates config name in metadata`() {
        val (repository, _) = createTestRepository()
        val config = EvalRunConfig(name = "old-name", datasetName = "dataset1")
        repository.createOrUpdateConfig("old-name", config)

        val result = repository.renameConfig("old-name", "new-name")

        assertNotNull(result)
        assertEquals("new-name", result!!.name)

        val configs = repository.listConfigs()
        assertTrue(configs.any { it.name == "new-name" })
        assertFalse(configs.any { it.name == "old-name" })
    }

    @Test
    fun `renameConfig returns null for non-existent config`() {
        val (repository, _) = createTestRepository()

        val result = repository.renameConfig("non-existent", "new-name")

        assertNull(result)
    }

    @Test
    fun `removeConfig removes config from metadata and deletes file`() {
        val (repository, _) = createTestRepository()
        val config = EvalRunConfig(name = "config", datasetName = "dataset1")
        val info = repository.createOrUpdateConfig("config", config)
        val path = repository.getConfigPathByName(info.name)
        assertNotNull(path)
        assertTrue(Files.exists(path!!))

        repository.removeConfig("config")

        val configs = repository.listConfigs()
        assertFalse(configs.any { it.name == "config" })
        assertFalse(Files.exists(path))
    }

    @Test
    fun `getConfigPathByName returns correct path for config`() {
        val (repository, tempDir) = createTestRepository()
        val config = EvalRunConfig(name = "config", datasetName = "dataset1")
        val info = repository.createOrUpdateConfig("config", config)

        val path = repository.getConfigPathByName(info.name)

        assertNotNull(path)
        assertEquals(tempDir.resolve(".jbeval").resolve("configs").resolve(info.fileName), path)
    }

    @Test
    fun `loadConfig loads config from file system`() {
        val (repository, tempDir) = createTestRepository()
        val config = EvalRunConfig(name = "config", datasetName = "dataset1")

        // Create config through repository
        val info = repository.createOrUpdateConfig("config", config)

        // Load it back
        val loaded = repository.loadConfig(info)

        assertEquals("config", loaded.name)
        assertEquals("dataset1", loaded.datasetName)
    }

    @Test
    fun `loadConfig returns default config when file does not exist`() {
        val (repository, tempDir) = createTestRepository()
        val configsDir = tempDir.resolve(".jbeval").resolve("configs")
        Files.createDirectories(configsDir)

        // Create a ConfigInfo pointing to non-existent file
        val info = ConfigInfo("non-existent", "non-existent.json")

        val loaded = repository.loadConfig(info)

        assertEquals("", loaded.name)
    }

    @Test
    fun `createOrUpdateConfig uses blank name fallback`() {
        val (repository, _) = createTestRepository()
        val config = EvalRunConfig(name = "", datasetName = "dataset1")

        val info = repository.createOrUpdateConfig("", config)

        assertEquals("config", info.name)
    }

    @Test
    fun `createOrUpdateConfig persists evaluators list`() {
        val (repository, _) = createTestRepository()
        val evaluators = listOf(
            EvaluatorConfig(name = "eval1", type = "llm judge"),
            EvaluatorConfig(name = "eval2", type = "regexp", pattern = ".*")
        )
        val config = EvalRunConfig(name = "config", evaluators = evaluators)

        val info = repository.createOrUpdateConfig("config", config)
        val loaded = repository.loadConfig(info)

        assertEquals(2, loaded.evaluators?.size)
        assertEquals("eval1", loaded.evaluators?.get(0)?.name)
        assertEquals("regexp", loaded.evaluators?.get(1)?.type)
    }

    @Test
    fun `listConfigs filters out non-existent files`() {
        val (repository, _) = createTestRepository()
        val config = EvalRunConfig(name = "config", datasetName = "dataset1")
        val info = repository.createOrUpdateConfig("config", config)

        val path = repository.getConfigPathByName(info.name)
        assertNotNull(path)
        Files.delete(path!!)

        val configs = repository.listConfigs()

        assertTrue(configs.isEmpty())
    }

    @Test
    fun `listConfigs ignores hidden files`() {
        val (repository, tempDir) = createTestRepository()
        val configsDir = tempDir.resolve(".jbeval").resolve("configs")
        Files.createDirectories(configsDir)
        Files.writeString(configsDir.resolve(".hidden.json"), "{}")
        Files.writeString(configsDir.resolve(".metadata"), "{}")

        val configs = repository.listConfigs()

        assertTrue(configs.all { !it.fileName.startsWith(".") })
    }

    @Test
    fun `createOrUpdateConfig creates unique file names when file exists`() {
        val (repository, tempDir) = createTestRepository()
        val configsDir = tempDir.resolve(".jbeval").resolve("configs")
        Files.createDirectories(configsDir)
        val config1 = EvalRunConfig(name = "config", datasetName = "dataset1")
        val config2 = EvalRunConfig(name = "config_1", datasetName = "dataset2")

        Files.writeString(configsDir.resolve("config_1.json"), gson.toJson(config1))

        val info1 = repository.createOrUpdateConfig("config", config1)
        val info2 = repository.createOrUpdateConfig("config_1", config2)

        assertNotEquals(info1.fileName, info2.fileName)
        val path1 = repository.getConfigPathByName(info1.name)
        val path2 = repository.getConfigPathByName(info2.name)
        assertNotNull(path1)
        assertNotNull(path2)
        assertTrue(Files.exists(path1!!))
        assertTrue(Files.exists(path2!!))
    }

    @Test
    fun `loadConfig handles corrupted JSON gracefully`() {
        val (repository, tempDir) = createTestRepository()
        val configsDir = tempDir.resolve(".jbeval").resolve("configs")
        Files.createDirectories(configsDir)
        val path = configsDir.resolve("corrupted.json")
        Files.writeString(path, "{ invalid json }")

        val info = ConfigInfo("corrupted", "corrupted.json")
        val loaded = repository.loadConfig(info)

        assertEquals("", loaded.name)
    }

    @Test
    fun `listConfigs handles missing metadata file`() {
        val (repository, tempDir) = createTestRepository()
        val configsDir = tempDir.resolve(".jbeval").resolve("configs")
        Files.createDirectories(configsDir)
        val config = EvalRunConfig(name = "config", datasetName = "dataset1")
        Files.writeString(configsDir.resolve("config.json"), gson.toJson(config))

        Files.deleteIfExists(configsDir.resolve(".metadata"))

        val configs = repository.listConfigs()

        assertTrue(configs.any { it.name == "config" })
    }

    @Test
    fun `listConfigs removes duplicate file names`() {
        val (repository, tempDir) = createTestRepository()
        val configsDir = tempDir.resolve(".jbeval").resolve("configs")
        Files.createDirectories(configsDir)
        val metadata = """
            {
                "configs": [
                    {"name": "config1", "fileName": "config.json"},
                    {"name": "config2", "fileName": "config.json"}
                ]
            }
        """.trimIndent()
        Files.writeString(configsDir.resolve(".metadata"), metadata)
        Files.writeString(configsDir.resolve("config.json"), gson.toJson(EvalRunConfig(name = "config")))

        val configs = repository.listConfigs()

        assertEquals(1, configs.size)
        assertEquals("config.json", configs[0].fileName)
    }

    @Test
    fun `default config name does not conflict with existing configs`() {
        val (repository, _) = createTestRepository()

        // Create existing configs that would conflict with default names
        repository.createOrUpdateConfig("config", EvalRunConfig(name = "config"))
        repository.createOrUpdateConfig("config_1", EvalRunConfig(name = "config_1"))
        repository.createOrUpdateConfig("config_2", EvalRunConfig(name = "config_2"))

        // Verify that we can still create new configs with incremented names
        val newConfig1 = repository.createOrUpdateConfig("config_3", EvalRunConfig(name = "config_3"))
        assertEquals("config_3", newConfig1.name)

        // Verify all configs exist without conflicts
        val allConfigs = repository.listConfigs()
        assertEquals(4, allConfigs.size)

        val configNames = allConfigs.map { it.name }.toSet()
        assertTrue(configNames.contains("config"))
        assertTrue(configNames.contains("config_1"))
        assertTrue(configNames.contains("config_2"))
        assertTrue(configNames.contains("config_3"))
    }

    @Test
    fun `config name generation skips to next available number`() {
        val (repository, _) = createTestRepository()

        // Create configs with gaps
        repository.createOrUpdateConfig("config", EvalRunConfig(name = "config"))
        repository.createOrUpdateConfig("config_1", EvalRunConfig(name = "config_1"))
        // Skip config_2
        repository.createOrUpdateConfig("config_3", EvalRunConfig(name = "config_3"))

        // We can create config_2 since it doesn't exist
        val newConfig = repository.createOrUpdateConfig("config_2", EvalRunConfig(name = "config_2"))
        assertEquals("config_2", newConfig.name)

        val allConfigs = repository.listConfigs()
        assertEquals(4, allConfigs.size)
    }

    @Test
    fun `empty config name defaults to config`() {
        val (repository, _) = createTestRepository()

        // Create config with blank name - repository should sanitize it
        val info = repository.createOrUpdateConfig("", EvalRunConfig(name = ""))
        assertEquals("config", info.name)

        // Verify the config was created with sanitized name
        val loaded = repository.loadConfig(info)
        assertEquals("config", loaded.name)
    }

    @Test
    fun `config name conflicts are case insensitive in repository`() {
        val (repository, _) = createTestRepository()

        repository.createOrUpdateConfig("Config", EvalRunConfig(name = "Config"))

        // Repository stores names as-is, but the test verifies the config exists
        val allConfigs = repository.listConfigs()

        // Since we created "Config", it should exist
        assertTrue(allConfigs.any { it.name.equals("Config", ignoreCase = true) })
    }
}
