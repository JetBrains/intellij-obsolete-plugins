package com.intellij.aidebugger.common.models.data

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.intellij.aidebugger.common.utility.JsonPathUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object JsonDatasetReader {
    fun readJson(path: Path): List<Pair<String, String>> {
        Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
            val root = JsonParser.parseReader(reader)
            return parseRoot(root)
        }
    }

    private fun parseRoot(root: JsonElement): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        when {
            root.isJsonArray -> {
                val arr = root.asJsonArray
                for (elem in arr) {
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        val input = stringLike(obj["input"]) ?: stringLike(obj["prompt"]) ?: stringLike(obj["question"]) ?: ""
                        val expected = stringLike(obj["expectedOutput"]) ?: stringLike(obj["expected"]) ?: stringLike(obj["output"]) ?: ""
                        if (input.isNotEmpty() || expected.isNotEmpty()) out.add(input to expected)
                    } else if (elem.isJsonPrimitive) {
                        out.add(elem.asString to "")
                    }
                }
            }
            root.isJsonObject -> {
                val obj = root.asJsonObject
                val items = obj.get("items")
                if (items != null && items.isJsonArray) {
                    for (it in items.asJsonArray) {
                        if (!it.isJsonObject) continue
                        val item = it.asJsonObject
                        val input = stringLike(item.get("input")) ?: ""
                        val expected = stringLike(item.get("output")) ?: ""
                        if (input.isNotEmpty() || expected.isNotEmpty()) out.add(input to expected)
                    }
                    return out
                }
                val inputArr = obj.get("input")?.takeIf { it.isJsonArray }?.asJsonArray
                val expectedArr = (obj.get("expectedOutput")?.takeIf { it.isJsonArray }
                    ?: obj.get("expected")?.takeIf { it.isJsonArray })?.asJsonArray
                if (inputArr != null) {
                    val max = maxOf(inputArr.size(), expectedArr?.size() ?: 0)
                    for (i in 0 until max) {
                        val inp = if (i < inputArr.size()) stringLike(inputArr.get(i)) else null
                        val exp = expectedArr?.let { if (i < it.size()) stringLike(it.get(i)) else null }
                        val inps = inp ?: ""
                        val exps = exp ?: ""
                        if (inps.isNotEmpty() || exps.isNotEmpty()) out.add(inps to exps)
                    }
                    return out
                }
                val keysAreIndexes = obj.entrySet().all { it.key.toIntOrNull() != null }
                if (keysAreIndexes) {
                    obj.entrySet().sortedBy { it.key.toInt() }.forEach { e ->
                        val inp = stringLike(e.value) ?: ""
                        if (inp.isNotEmpty()) out.add(inp to "")
                    }
                    return out
                }
            }
        }
        return out
    }

    private fun stringLike(el: JsonElement?): String? {
        if (el == null || el.isJsonNull) return null

        if (el.isJsonArray) {
            return el.asJsonArray.joinToString(" ") {
                JsonPathUtils.jsonStringLike(it) ?: ""
            }
        }

        return JsonPathUtils.jsonStringLike(el)
    }
}