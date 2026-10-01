package com.intellij.aidebugger.evaluationCli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.aidebugger.evaluation.models.entities.DataPoint
import com.intellij.aidebugger.evaluation.models.entities.EvalRunConfig
import com.intellij.aidebugger.evaluation.models.entities.EvaluatorConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class MainTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  private lateinit var projectDir: Path
  private lateinit var jbevalDir: Path
  private lateinit var mapper: ObjectMapper

  @Before
  fun setup() {
    projectDir = tempFolder.root.toPath()
    jbevalDir = projectDir.resolve(".jbeval")
    mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    Files.createDirectories(jbevalDir.resolve("remote/next"))
    Files.createDirectories(jbevalDir.resolve("remote/out"))
    Files.createDirectories(jbevalDir.resolve("eval"))
    Files.createDirectories(jbevalDir.resolve("datasets"))
    Files.createDirectories(jbevalDir.resolve("configs"))
    Files.createDirectories(projectDir.resolve("python"))
  }

  @Test
  fun `test EvaluationCli version constant`() {
    assertEquals("0.2.0", EvaluationCli.VERSION)
  }

  @Test
  fun `test DataPoint serialization and deserialization`() {
    val dataPoint = DataPoint(
      id = "1",
      input = "test input",
      outputGen = "generated output",
      outputExpected = "expected output",
      experimentId = "exp_001",
      raw = mapOf("key" to "value"),
      runDatetime = "2025-01-01T00:00:00",
      exception = null
    )

    val json = mapper.writeValueAsString(dataPoint)
    val deserialized: DataPoint = mapper.readValue(json)

    assertEquals(dataPoint.id, deserialized.id)
    assertEquals(dataPoint.input, deserialized.input)
    assertEquals(dataPoint.outputGen, deserialized.outputGen)
    assertEquals(dataPoint.outputExpected, deserialized.outputExpected)
    assertEquals(dataPoint.experimentId, deserialized.experimentId)
    assertEquals(dataPoint.runDatetime, deserialized.runDatetime)
  }

  @Test
  fun `test EvalRunConfig serialization and deserialization`() {
    val evaluatorConfig = EvaluatorConfig(
      name = "TestEvaluator",
      type = "llm judge",
      pattern = null,
      prompt = "Evaluate: {input}"
    )

    val config = EvalRunConfig(
      name = "test-eval",
      description = "Test evaluation",
      datasetName = "test-dataset",
      extractorClass = "com.intellij.aidebugger.evaluation.extractor.DebuggerTracesExtractor",
      runConfigName = "main",
      modelName = "gpt-4o-mini",
      modelParams = mapOf("temperature" to "0.0"),
      promptTemplate = "Test prompt",
      apiKey = "test-key",
      evaluators = listOf(evaluatorConfig)
    )

    val json = mapper.writeValueAsString(config)
    val deserialized: EvalRunConfig = mapper.readValue(json)

    assertEquals(config.name, deserialized.name)
    assertEquals(config.description, deserialized.description)
    assertEquals(config.datasetName, deserialized.datasetName)
    assertEquals(config.modelName, deserialized.modelName)
    assertEquals(config.evaluators?.size, deserialized.evaluators?.size)
  }

  @Test
  fun `test DataPoint with exception field`() {
    val dataPoint = DataPoint(
      id = "2",
      input = "failing input",
      outputGen = "",
      outputExpected = "expected",
      experimentId = "exp_002",
      raw = mapOf("error" to "timeout"),
      exception = "Process timed out"
    )

    assertNotNull(dataPoint.exception)
    assertEquals("Process timed out", dataPoint.exception)
    assertTrue(dataPoint.outputGen.isEmpty())
  }

  @Test
  fun `test DataPoint with complex raw data`() {
    val complexRaw = mapOf(
      "rootEvents" to listOf(
        mapOf(
          "type" to "LANGGRAPH_NODE_ENTER",
          "name" to "agent",
          "timestamp_ms" to 1234567890
        ),
        mapOf(
          "type" to "LANGGRAPH_NODE_EXIT",
          "name" to "agent",
          "timestamp_ms" to 1234567900,
          "outputs" to mapOf("result" to "success")
        )
      )
    )

    val dataPoint = DataPoint(
      id = "3",
      input = "complex test",
      outputGen = "output",
      outputExpected = "expected",
      experimentId = "exp_003",
      raw = complexRaw
    )

    assertNotNull(dataPoint.raw["rootEvents"])
    assertTrue(dataPoint.raw["rootEvents"] is List<*>)
  }

  @Test
  fun `test EvaluatorConfig with regexp type`() {
    val config = EvaluatorConfig(
      name = "RegexpEvaluator",
      type = "regexp",
      pattern = ".*{outputExpected}.*"
    )

    assertEquals("regexp", config.type)
    assertNotNull(config.pattern)
    assertTrue(config.validatePrompt())
  }

  @Test
  fun `test EvaluatorConfig with invalid prompt variables`() {
    val config = EvaluatorConfig(
      name = "BadEvaluator",
      type = "llm judge",
      prompt = "Test {invalidVar} and {anotherInvalid}"
    )

    assertFalse(config.validatePrompt())
  }

  @Test
  fun `test EvaluatorConfig default name for type`() {
    assertEquals("LLMJudge", EvaluatorConfig.getDefaultNameForType("llm judge"))
    assertEquals("Regexp", EvaluatorConfig.getDefaultNameForType("regexp"))
    assertEquals("Evaluator", EvaluatorConfig.getDefaultNameForType("custom"))
  }

  @Test
  fun `test EvalRunConfig with default values`() {
    val config = EvalRunConfig()

    assertEquals("", config.name)
    assertNull(config.description)
    assertEquals(EvalRunConfig.DEFAULT_EXTRACTOR_FQCN, config.extractorClass)
    assertEquals("", config.runConfigName)
  }

  @Test
  fun `test multiple DataPoints serialization as list`() {
    val dataPoints = listOf(
      DataPoint("1", "input1", "gen1", "exp1", "exp_001"),
      DataPoint("2", "input2", "gen2", "exp2", "exp_001"),
      DataPoint("3", "input3", "gen3", "exp3", "exp_001")
    )

    val json = mapper.writeValueAsString(dataPoints)
    val deserialized: List<DataPoint> = mapper.readValue(json)

    assertEquals(3, deserialized.size)
    assertEquals("1", deserialized[0].id)
    assertEquals("2", deserialized[1].id)
    assertEquals("3", deserialized[2].id)
  }

  @Test
  fun `test DataPoint with empty raw map`() {
    val dataPoint = DataPoint(
      id = "4",
      input = "test",
      outputGen = "output",
      outputExpected = "expected",
      experimentId = "exp_004",
      raw = emptyMap()
    )

    assertTrue(dataPoint.raw.isEmpty())
    assertNotNull(dataPoint.raw)
  }

  @Test
  fun `test EvalRunConfig with multiple evaluators`() {
    val evaluators = listOf(
      EvaluatorConfig("Eval1", "llm judge", null, "Prompt 1"),
      EvaluatorConfig("Eval2", "regexp", "pattern.*", null),
      EvaluatorConfig("Eval3", "llm judge", null, "Prompt 3")
    )

    val config = EvalRunConfig(
      name = "multi-eval",
      evaluators = evaluators
    )

    assertEquals(3, config.evaluators?.size)
    assertEquals("Eval1", config.evaluators?.get(0)?.name)
    assertEquals("regexp", config.evaluators?.get(1)?.type)
  }

  @Test
  fun `test DataPoint with null optional fields`() {
    val dataPoint = DataPoint(
      id = "5",
      input = "input",
      outputGen = "gen",
      outputExpected = "expected",
      experimentId = "exp_005",
      runDatetime = null,
      exception = null
    )

    assertNull(dataPoint.runDatetime)
    assertNull(dataPoint.exception)
  }

  @Test
  fun `test EvalRunConfig with model params as map`() {
    val config = EvalRunConfig(
      name = "model-test",
      modelName = "gpt-4",
      modelParams = mapOf(
        "temperature" to "0.7",
        "max_tokens" to "2000",
        "top_p" to "0.9"
      )
    )

    assertEquals("gpt-4", config.modelName)
    assertEquals(3, config.modelParams?.size)
    assertEquals("0.7", config.modelParams?.get("temperature"))
  }

  @Test
  fun `test timestamp format generation`() {
    val timestamp = java.time.LocalDateTime.now()
      .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

    assertNotNull(timestamp)
    assertTrue(timestamp.matches(Regex("\\d{8}_\\d{6}")))
  }

  @Test
  fun `test EvaluatorConfig allowed prompt vars contains expected fields`() {
    val allowedVars = EvaluatorConfig.ALLOWED_PROMPT_VARS

    assertTrue(allowedVars.contains("id"))
    assertTrue(allowedVars.contains("input"))
    assertTrue(allowedVars.contains("outputGen"))
    assertTrue(allowedVars.contains("outputExpected"))
    assertTrue(allowedVars.contains("experimentId"))
    assertTrue(allowedVars.contains("raw"))
    assertTrue(allowedVars.contains("runDatetime"))
    assertTrue(allowedVars.contains("exception"))
  }

  @Test
  fun `test DataPoint serialization preserves all fields`() {
    val now = java.time.LocalDateTime.now().toString()
    val dataPoint = DataPoint(
      id = "test-001",
      input = "What is Kotlin?",
      outputGen = "Kotlin is a programming language",
      outputExpected = "Kotlin is a modern JVM language",
      experimentId = "exp_20250101",
      raw = mapOf("metadata" to "test", "count" to 42),
      runDatetime = now,
      exception = null
    )

    val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataPoint)

    assertTrue(json.contains("test-001"))
    assertTrue(json.contains("What is Kotlin?"))
    assertTrue(json.contains("exp_20250101"))
    assertTrue(json.contains(now))
  }
}
