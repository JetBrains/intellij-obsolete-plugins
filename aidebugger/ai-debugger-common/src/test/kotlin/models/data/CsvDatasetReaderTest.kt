package com.intellij.aidebugger.common.models.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class CsvDatasetReaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `reads evaluation dataset with standard headers`() {
        val csv = "input,expectedOutput\nWhat is 2+2?,4\nCapital of France?,Paris"
        val file = createCsvFile(csv)

        val result = CsvDatasetReader.readCsv(file)

        assertEquals(2, result.size)
        assertEquals("What is 2+2?" to "4", result[0])
        assertEquals("Capital of France?" to "Paris", result[1])
    }

    @Test
    fun `reads dataset with expected header alias`() {
        val csv = "input,expected\nAnalyze the code,Here is the analysis\nFix the bug,Applied the fix"
        val file = createCsvFile(csv)

        val result = CsvDatasetReader.readCsv(file)

        assertEquals(2, result.size)
        assertEquals("Analyze the code" to "Here is the analysis", result[0])
        assertEquals("Fix the bug" to "Applied the fix", result[1])
    }

    @Test
    fun `handles prompts with commas in quotes`() {
        val csv = "input,expectedOutput\n\"Write a function that takes x, y, and z\",\"def func(x, y, z): pass\""
        val file = createCsvFile(csv)

        val result = CsvDatasetReader.readCsv(file)

        assertEquals(1, result.size)
        assertEquals("Write a function that takes x, y, and z" to "def func(x, y, z): pass", result[0])
    }

    @Test
    fun `handles multiline prompts and responses`() {
        val csv = "input,expectedOutput\n\"Debug this code:\ndef foo():\n  return x\",\"The variable x is not defined\""
        val file = createCsvFile(csv)

        val result = CsvDatasetReader.readCsv(file)

        assertEquals(1, result.size)
        val (input, output) = result[0]
        assertTrue(input.contains("Debug this code:"))
        assertTrue(input.contains("def foo():"))
        assertEquals("The variable x is not defined", output)
    }

    @Test
    fun `handles JSON strings in cells`() {
        val csv = "input,expectedOutput\n\"{\"\"query\"\": \"\"test\"\"}\",\"{\"\"result\"\": \"\"ok\"\"}\""
        val file = createCsvFile(csv)

        val result = CsvDatasetReader.readCsv(file)

        assertEquals(1, result.size)
        assertEquals("{\"query\": \"test\"}" to "{\"result\": \"ok\"}", result[0])
    }

    @Test
    fun `handles case insensitive headers`() {
        val csv = "INPUT,EXPECTEDOUTPUT\ntest input,test output"
        val file = createCsvFile(csv)

        val result = CsvDatasetReader.readCsv(file)

        assertEquals(1, result.size)
        assertEquals("test input" to "test output", result[0])
    }

    @Test
    fun `handles dataset without headers using column order`() {
        val csv = "Explain Python decorators,Decorators are callable wrappers\nWhat is a closure?,A closure is a nested function"
        val file = createCsvFile(csv)

        val result = CsvDatasetReader.readCsv(file)

        assertEquals(2, result.size)
        assertEquals("Explain Python decorators" to "Decorators are callable wrappers", result[0])
        assertEquals("What is a closure?" to "A closure is a nested function", result[1])
    }

    @Test
    fun `skips empty rows in dataset`() {
        val csv = "input,expectedOutput\nFirst prompt,First response\n,\nSecond prompt,Second response"
        val file = createCsvFile(csv)

        val result = CsvDatasetReader.readCsv(file)

        assertEquals(2, result.size)
        assertEquals("First prompt" to "First response", result[0])
        assertEquals("Second prompt" to "Second response", result[1])
    }

    @Test
    fun `handles row with missing expected output`() {
        val csv = "input,expectedOutput\nPrompt with response,Expected response\nPrompt without response"
        val file = createCsvFile(csv)

        val result = CsvDatasetReader.readCsv(file)

        assertEquals(2, result.size)
        assertEquals("Prompt with response" to "Expected response", result[0])
        assertEquals("Prompt without response" to "", result[1])
    }

    @Test
    fun `handles empty CSV file`() {
        val csv = ""
        val file = createCsvFile(csv)

        val result = CsvDatasetReader.readCsv(file)

        assertEquals(0, result.size)
    }

    @Test
    fun `handles long text content typical for LLM evaluation`() {
        val longInput = "Analyze the following code and suggest improvements for readability and performance"
        val longOutput = "The code can be improved by: 1. Using more descriptive variable names 2. Adding error handling"
        val csv = "input,expectedOutput\n\"$longInput\",\"$longOutput\""
        val file = createCsvFile(csv)

        val result = CsvDatasetReader.readCsv(file)

        assertEquals(1, result.size)
        assertEquals(longInput to longOutput, result[0])
    }

    private fun createCsvFile(content: String) = tempFolder.root.toPath().resolve("test.csv").also {
        Files.writeString(it, content)
    }
}
