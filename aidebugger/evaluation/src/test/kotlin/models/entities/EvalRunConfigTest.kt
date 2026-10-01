package com.intellij.aidebugger.evaluation.models.entities

import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvalRunConfigTest {

    @Test
    fun `EvalRunConfig has correct default values`() {
        val config = EvalRunConfig()

        assertEquals("", config.name)
        assertNull(config.description)
        assertNull(config.datasetName)
        assertEquals(EvalRunConfig.DEFAULT_EXTRACTOR_FQCN, config.extractorClass)
        assertNull(config.configPath)
        assertNull(config.extractorOutputPath)
        assertNull(config.datasetMapping)
        assertEquals("", config.runConfigName)
        assertNull(config.providerInstanceId)
        assertNull(config.providerType)
        assertNull(config.modelName)
        assertNull(config.modelParams)
        assertNull(config.promptTemplate)
        assertNull(config.apiKey)
        assertNull(config.evaluators)
    }

    @Test
    fun `name field can be set and retrieved`() {
        val config = EvalRunConfig()
        config.name = "Test Config"
        assertEquals("Test Config", config.name)
    }

    @Test
    fun `name field validates non-empty requirement`() {
        val config = EvalRunConfig(name = "Valid Name")
        assertTrue(config.name.isNotEmpty())
    }

    @Test
    fun `description field can be null`() {
        val config = EvalRunConfig(description = null)
        assertNull(config.description)
    }

    @Test
    fun `description field can be set`() {
        val config = EvalRunConfig(description = "Test description")
        assertEquals("Test description", config.description)
    }

    @Test
    fun `datasetName field can be null`() {
        val config = EvalRunConfig(datasetName = null)
        assertNull(config.datasetName)
    }

    @Test
    fun `datasetName field can be set`() {
        val config = EvalRunConfig(datasetName = "my-dataset")
        assertEquals("my-dataset", config.datasetName)
    }

    @Test
    fun `extractorClass has correct default value`() {
        val config = EvalRunConfig()
        assertEquals("com.intellij.aidebugger.evaluation.extractor.DebuggerTracesExtractor", config.extractorClass)
    }

    @Test
    fun `extractorClass can be customized`() {
        val config = EvalRunConfig(extractorClass = "com.example.CustomExtractor")
        assertEquals("com.example.CustomExtractor", config.extractorClass)
    }

    @Test
    fun `configPath can be null`() {
        val config = EvalRunConfig(configPath = null)
        assertNull(config.configPath)
    }

    @Test
    fun `configPath can be set`() {
        val config = EvalRunConfig(configPath = "/path/to/config.json")
        assertEquals("/path/to/config.json", config.configPath)
    }

    @Test
    fun `extractorOutputPath can be null`() {
        val config = EvalRunConfig(extractorOutputPath = null)
        assertNull(config.extractorOutputPath)
    }

    @Test
    fun `extractorOutputPath can be set`() {
        val config = EvalRunConfig(extractorOutputPath = "/path/to/output")
        assertEquals("/path/to/output", config.extractorOutputPath)
    }

    @Test
    fun `datasetMapping can be null`() {
        val config = EvalRunConfig(datasetMapping = null)
        assertNull(config.datasetMapping)
    }

    @Test
    fun `datasetMapping can be set with various types`() {
        val mapping = mapOf(
            "stringValue" to "test",
            "numberValue" to 42,
            "boolValue" to true,
            "nullValue" to null
        )
        val config = EvalRunConfig(datasetMapping = mapping)
        
        assertEquals("test", config.datasetMapping?.get("stringValue"))
        assertEquals(42, config.datasetMapping?.get("numberValue"))
        assertEquals(true, config.datasetMapping?.get("boolValue"))
        assertNull(config.datasetMapping?.get("nullValue"))
    }

    @Test
    fun `runConfigName can be empty by default`() {
        val config = EvalRunConfig()
        assertEquals("", config.runConfigName)
    }

    @Test
    fun `runConfigName can be set`() {
        val config = EvalRunConfig(runConfigName = "main")
        assertEquals("main", config.runConfigName)
    }

    @Test
    fun `providerInstanceId can be null`() {
        val config = EvalRunConfig(providerInstanceId = null)
        assertNull(config.providerInstanceId)
    }

    @Test
    fun `providerInstanceId can be set`() {
        val config = EvalRunConfig(providerInstanceId = "provider-123")
        assertEquals("provider-123", config.providerInstanceId)
    }

    @Test
    fun `providerType can be null`() {
        val config = EvalRunConfig(providerType = null)
        assertNull(config.providerType)
    }

    @Test
    fun `providerType can be set to OpenAI`() {
        val config = EvalRunConfig(providerType = LlmProviderType.OPENAI)
        assertEquals(LlmProviderType.OPENAI, config.providerType)
    }

    @Test
    fun `providerType can be set to Gemini`() {
        val config = EvalRunConfig(providerType = LlmProviderType.GEMINI)
        assertEquals(LlmProviderType.GEMINI, config.providerType)
    }

    @Test
    fun `modelName can be null`() {
        val config = EvalRunConfig(modelName = null)
        assertNull(config.modelName)
    }

    @Test
    fun `modelName can be set`() {
        val config = EvalRunConfig(modelName = "gpt-4o-mini")
        assertEquals("gpt-4o-mini", config.modelName)
    }

    @Test
    fun `modelParams can be null`() {
        val config = EvalRunConfig(modelParams = null)
        assertNull(config.modelParams)
    }

    @Test
    fun `modelParams can be set with multiple parameters`() {
        val params = mapOf(
            "temperature" to "0.7",
            "max_tokens" to "2000",
            "top_p" to "0.9"
        )
        val config = EvalRunConfig(modelParams = params)
        
        assertEquals("0.7", config.modelParams?.get("temperature"))
        assertEquals("2000", config.modelParams?.get("max_tokens"))
        assertEquals("0.9", config.modelParams?.get("top_p"))
    }

    @Test
    fun `promptTemplate can be null`() {
        val config = EvalRunConfig(promptTemplate = null)
        assertNull(config.promptTemplate)
    }

    @Test
    fun `promptTemplate can be set`() {
        val template = "Answer the following: {input}"
        val config = EvalRunConfig(promptTemplate = template)
        assertEquals(template, config.promptTemplate)
    }

    @Test
    fun `apiKey can be null`() {
        val config = EvalRunConfig(apiKey = null)
        assertNull(config.apiKey)
    }

    @Test
    fun `apiKey can be set`() {
        val config = EvalRunConfig(apiKey = "sk-test-key")
        assertEquals("sk-test-key", config.apiKey)
    }

    @Test
    fun `evaluators can be null`() {
        val config = EvalRunConfig(evaluators = null)
        assertNull(config.evaluators)
    }

    @Test
    fun `evaluators can be set with single evaluator`() {
        val evaluator = EvaluatorConfig(
            name = "TestEvaluator",
            type = "llm judge"
        )
        val config = EvalRunConfig(evaluators = listOf(evaluator))
        
        assertEquals(1, config.evaluators?.size)
        assertEquals("TestEvaluator", config.evaluators?.get(0)?.name)
    }

    @Test
    fun `evaluators can be set with multiple evaluators`() {
        val evaluators = listOf(
            EvaluatorConfig(name = "Eval1", type = "llm judge"),
            EvaluatorConfig(name = "Eval2", type = "regexp"),
            EvaluatorConfig(name = "Eval3", type = "llm judge")
        )
        val config = EvalRunConfig(evaluators = evaluators)
        
        assertEquals(3, config.evaluators?.size)
        assertEquals("Eval1", config.evaluators?.get(0)?.name)
        assertEquals("Eval2", config.evaluators?.get(1)?.name)
        assertEquals("Eval3", config.evaluators?.get(2)?.name)
    }

    @Test
    fun `evaluators can be empty list`() {
        val config = EvalRunConfig(evaluators = emptyList())
        
        assertNotNull(config.evaluators)
        assertTrue(config.evaluators!!.isEmpty())
    }

    @Test
    fun `complete configuration can be created`() {
        val config = EvalRunConfig(
            name = "Complete Test Config",
            description = "A complete evaluation configuration",
            datasetName = "test-dataset",
            extractorClass = "com.example.CustomExtractor",
            configPath = "/path/to/config",
            extractorOutputPath = "/path/to/output",
            datasetMapping = mapOf("input" to "question", "output" to "answer"),
            runConfigName = "main",
            providerInstanceId = "provider-001",
            providerType = LlmProviderType.OPENAI,
            modelName = "gpt-4o-mini",
            modelParams = mapOf("temperature" to "0.7"),
            promptTemplate = "Answer: {input}",
            apiKey = "sk-test",
            evaluators = listOf(
                EvaluatorConfig(name = "Judge1", type = "llm judge")
            )
        )

        assertEquals("Complete Test Config", config.name)
        assertEquals("A complete evaluation configuration", config.description)
        assertEquals("test-dataset", config.datasetName)
        assertEquals("com.example.CustomExtractor", config.extractorClass)
        assertEquals("/path/to/config", config.configPath)
        assertEquals("/path/to/output", config.extractorOutputPath)
        assertEquals(2, config.datasetMapping?.size)
        assertEquals("main", config.runConfigName)
        assertEquals("provider-001", config.providerInstanceId)
        assertEquals(LlmProviderType.OPENAI, config.providerType)
        assertEquals("gpt-4o-mini", config.modelName)
        assertEquals("0.7", config.modelParams?.get("temperature"))
        assertEquals("Answer: {input}", config.promptTemplate)
        assertEquals("sk-test", config.apiKey)
        assertEquals(1, config.evaluators?.size)
    }

    @Test
    fun `DEFAULT_EXTRACTOR_FQCN constant has correct value`() {
        assertEquals(
            "com.intellij.aidebugger.evaluation.extractor.DebuggerTracesExtractor",
            EvalRunConfig.DEFAULT_EXTRACTOR_FQCN
        )
    }

    @Test
    fun `mutable properties can be modified after creation`() {
        val config = EvalRunConfig()
        
        config.name = "Modified Name"
        config.description = "Modified Description"
        config.datasetName = "modified-dataset"
        config.runConfigName = "modified-run"
        
        assertEquals("Modified Name", config.name)
        assertEquals("Modified Description", config.description)
        assertEquals("modified-dataset", config.datasetName)
        assertEquals("modified-run", config.runConfigName)
    }
}
