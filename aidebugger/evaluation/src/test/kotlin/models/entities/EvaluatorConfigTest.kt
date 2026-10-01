package com.intellij.aidebugger.evaluation.models.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluatorConfigTest {

    @Test
    fun `validatePrompt returns true for valid llm judge prompt with score and explanation`() {
        val config = EvaluatorConfig(
            name = "TestJudge",
            type = "llm judge",
            prompt = """
                Evaluate the output:
                Input: {input}
                Generated: {outputGen}
                Expected: {outputExpected}
                
                Return JSON with "score": <0-1> and "explanation": <reasoning>
            """.trimIndent()
        )

        assertTrue(config.validatePrompt())
    }

    @Test
    fun `validatePrompt returns false for llm judge prompt without score`() {
        val config = EvaluatorConfig(
            name = "TestJudge",
            type = "llm judge",
            prompt = """
                Evaluate: {input}
                Provide "explanation": <reasoning>
            """.trimIndent()
        )

        assertFalse(config.validatePrompt())
    }

    @Test
    fun `validatePrompt returns false for llm judge prompt without explanation`() {
        val config = EvaluatorConfig(
            name = "TestJudge",
            type = "llm judge",
            prompt = """
                Evaluate: {input}
                Return "score": 0.5
            """.trimIndent()
        )

        assertFalse(config.validatePrompt())
    }

    @Test
    fun `validatePrompt returns false for llm judge with invalid template variables`() {
        val config = EvaluatorConfig(
            name = "TestJudge",
            type = "llm judge",
            prompt = """
                Evaluate: {input} and {invalidVar}
                Return "score": 0.5 and "explanation": "test"
            """.trimIndent()
        )

        assertFalse(config.validatePrompt())
    }

    @Test
    fun `validatePrompt returns true for llm judge with all allowed variables`() {
        val config = EvaluatorConfig(
            name = "TestJudge",
            type = "llm judge",
            prompt = """
                ID: {id}
                Input: {input}
                Output: {outputGen}
                Expected: {outputExpected}
                Experiment: {experimentId}
                Raw: {raw}
                Time: {runDatetime}
                Exception: {exception}
                
                Return "score": 0.5 and "explanation": "test"
            """.trimIndent()
        )

        assertTrue(config.validatePrompt())
    }

    @Test
    fun `validatePrompt returns true for regexp with valid pattern`() {
        val config = EvaluatorConfig(
            name = "TestRegexp",
            type = "regexp",
            pattern = ".*{outputExpected}.*"
        )

        assertTrue(config.validatePrompt())
    }

    @Test
    fun `validatePrompt returns false for regexp with invalid template variables`() {
        val config = EvaluatorConfig(
            name = "TestRegexp",
            type = "regexp",
            pattern = "{invalidVar}.*"
        )

        assertFalse(config.validatePrompt())
    }

    @Test
    fun `validatePrompt returns true for regexp with allowed variables`() {
        val config = EvaluatorConfig(
            name = "TestRegexp",
            type = "regexp",
            pattern = "{input}.*{outputGen}"
        )

        assertTrue(config.validatePrompt())
    }

    @Test
    fun `validatePrompt returns true when llm judge prompt is null`() {
        val config = EvaluatorConfig(
            name = "TestJudge",
            type = "llm judge",
            prompt = null
        )

        assertTrue(config.validatePrompt())
    }

    @Test
    fun `validatePrompt returns true when regexp pattern is null`() {
        val config = EvaluatorConfig(
            name = "TestRegexp",
            type = "regexp",
            pattern = null
        )

        assertTrue(config.validatePrompt())
    }

    @Test
    fun `validatePrompt returns true for unknown evaluator types`() {
        val config = EvaluatorConfig(
            name = "CustomEvaluator",
            type = "custom",
            prompt = "anything goes here"
        )

        assertTrue(config.validatePrompt())
    }

    @Test
    fun `validatePrompt detects score with different casing`() {
        val config = EvaluatorConfig(
            name = "TestJudge",
            type = "llm judge",
            prompt = """
                Return Score: 0.5 and Explanation: test
            """.trimIndent()
        )

        assertTrue(config.validatePrompt())
    }

    @Test
    fun `validatePrompt detects score in JSON format`() {
        val config = EvaluatorConfig(
            name = "TestJudge",
            type = "llm judge",
            prompt = """
                Return JSON: {"score": 0.5, "explanation": "test"}
            """.trimIndent()
        )

        assertTrue(config.validatePrompt())
    }

    @Test
    fun `getDefaultNameForType returns correct defaults`() {
        assertEquals("LLMJudge", EvaluatorConfig.getDefaultNameForType("llm judge"))
        assertEquals("Regexp", EvaluatorConfig.getDefaultNameForType("regexp"))
        assertEquals("Evaluator", EvaluatorConfig.getDefaultNameForType("custom"))
        assertEquals("Evaluator", EvaluatorConfig.getDefaultNameForType("unknown"))
    }

    @Test
    fun `ALLOWED_PROMPT_VARS contains all expected variables`() {
        val allowedVars = EvaluatorConfig.ALLOWED_PROMPT_VARS

        assertEquals(8, allowedVars.size)
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
    fun `EvaluatorConfig has correct default values`() {
        val config = EvaluatorConfig()

        assertEquals("LLMJudge-1", config.name)
        assertEquals("llm judge", config.type)
        assertNull(config.pattern)
        assertNull(config.prompt)
    }

    @Test
    fun `validatePrompt handles empty prompt string`() {
        val config = EvaluatorConfig(
            name = "TestJudge",
            type = "llm judge",
            prompt = ""
        )

        assertFalse(config.validatePrompt())
    }

    @Test
    fun `validatePrompt handles multiple template variable occurrences`() {
        val config = EvaluatorConfig(
            name = "TestJudge",
            type = "llm judge",
            prompt = """
                Compare {input} with {input} again.
                Check {outputGen} and {outputExpected}.
                Return "score": 0.5 and "explanation": "test"
            """.trimIndent()
        )

        assertTrue(config.validatePrompt())
    }

    @Test
    fun `validatePrompt handles template variables with underscores`() {
        val config = EvaluatorConfig(
            name = "TestRegexp",
            type = "regexp",
            pattern = "{output_expected}"
        )

        // output_expected is not in allowed vars (should be outputExpected)
        assertFalse(config.validatePrompt())
    }

    @Test
    fun `name field can be set and retrieved`() {
        val config = EvaluatorConfig()
        config.name = "CustomName"
        assertEquals("CustomName", config.name)
    }

    @Test
    fun `type field can be set and retrieved`() {
        val config = EvaluatorConfig()
        config.type = "custom"
        assertEquals("custom", config.type)
    }

    @Test
    fun `pattern field can be set and retrieved`() {
        val config = EvaluatorConfig()
        config.pattern = "test.*pattern"
        assertEquals("test.*pattern", config.pattern)
    }

    @Test
    fun `prompt field can be set and retrieved`() {
        val config = EvaluatorConfig()
        config.prompt = "Test prompt"
        assertEquals("Test prompt", config.prompt)
    }
}
