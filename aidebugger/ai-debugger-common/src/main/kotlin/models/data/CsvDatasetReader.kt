package com.intellij.aidebugger.common.models.data

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object CsvDatasetReader {
    private val INPUT_ALIASES = setOf("input")
    private val EXPECTED_ALIASES = setOf("expectedoutput", "expected")

    fun readCsv(path: Path): List<Pair<String, String>> {
        return Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
            val formatNoHeaders = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()

            val parser = CSVParser(reader, formatNoHeaders)
            val records = parser.records
            if (records.isEmpty()) return emptyList()

            val firstRecord = records.first()
            val hasHeaders = firstRecord.any { cell ->
                val normalized = cell.trim().lowercase()
                normalized in INPUT_ALIASES || normalized in EXPECTED_ALIASES
            }

            if (hasHeaders) {
                return Files.newBufferedReader(path, StandardCharsets.UTF_8).use { freshReader ->
                    val formatWithHeaders = CSVFormat.DEFAULT.builder()
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .build()

                    CSVParser(freshReader, formatWithHeaders).records.mapNotNull { record ->
                        val input = record.getByAliases(INPUT_ALIASES)
                        val expected = record.getByAliases(EXPECTED_ALIASES)

                        if (input.isNotEmpty() || expected.isNotEmpty()) {
                            input to expected
                        } else {
                            null
                        }
                    }
                }
            } else {
                return records.mapNotNull { record ->
                    val input = record.getOrEmpty(0)
                    val expected = record.getOrEmpty(1)

                    if (input.isNotEmpty() || expected.isNotEmpty()) {
                        input to expected
                    } else {
                        null
                    }
                }
            }
        }
    }

    private fun CSVRecord.getOrEmpty(index: Int): String =
        if (index in 0 until size()) get(index) ?: "" else ""

    private fun CSVRecord.getByAliases(aliases: Set<String>): String {
        val map = toMap()
        if (map.isEmpty()) return ""
        return map.entries.firstOrNull { (key, _) ->
            aliases.any { it.equals(key, ignoreCase = true) }
        }?.value ?: ""
    }
}