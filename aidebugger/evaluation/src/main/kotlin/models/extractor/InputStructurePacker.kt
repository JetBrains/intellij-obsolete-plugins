package com.intellij.aidebugger.evaluation.models.extractor

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.aidebugger.common.utility.GsonUtil
import com.intellij.aidebugger.common.utility.JsonPathToken
import com.intellij.aidebugger.common.utility.JsonPathUtils

object InputStructurePacker {

    fun packInput(path: String, value: String): String {
        val tokens = JsonPathUtils.parseJsonPath(path)
        val inputPropIdx = findInputSegmentIndex(tokens)
        if (inputPropIdx == -1 || inputPropIdx == tokens.size - 1) {
            return value
        }
        val suffixTokens = tokens.subList(inputPropIdx + 1, tokens.size)
        return buildStructureFromTokens(suffixTokens, value)
    }

    fun reconstructInput(path: String, value: String, inputStruct: String?): String {
        if (inputStruct.isNullOrBlank()) {
            return packInput(path, value)
        }

        try {
            val structElement = GsonUtil.parseString(inputStruct)
            val tokens = JsonPathUtils.parseJsonPath(path)
            val inputPropIdx = findInputSegmentIndex(tokens)

            if (inputPropIdx == -1 || inputPropIdx == tokens.size - 1) {
                return value
            }

            val suffixTokens = tokens.subList(inputPropIdx + 1, tokens.size)
            val updated = insertValueIntoStructure(structElement, suffixTokens, value)
            return GsonUtil.gson.toJson(updated)
        } catch (_: Throwable) {
            return packInput(path, value)
        }
    }

    private fun findInputSegmentIndex(tokens: List<JsonPathToken>): Int {
        return tokens.indexOfLast { token ->
            token is JsonPathToken.Segment && (token.name == "input" || token.name == "inputs")
        }
    }

    private fun buildStructureFromTokens(tokens: List<JsonPathToken>, leafValue: String): String {
        if (tokens.isEmpty()) return leafValue

        var current: Any = JsonPrimitive(leafValue)
        for (i in tokens.indices.reversed()) {
            val token = tokens[i]
            current = when (token) {
                is JsonPathToken.Segment -> {
                    JsonObject().apply {
                        add(token.name, current.toJsonElement())
                    }
                }
                is JsonPathToken.Idx -> {
                    JsonArray().apply {
                        add(current.toJsonElement())
                    }
                }
            }
        }

        return when (current) {
            is JsonObject -> current.toString()
            is JsonArray -> current.toString()
            else -> leafValue
        }
    }

    private fun Any.toJsonElement(): JsonElement = when (this) {
        is JsonElement -> this
        else -> GsonUtil.toJsonElement(this)
    }

    private fun insertValueIntoStructure(
        root: JsonElement,
        tokens: List<JsonPathToken>,
        value: String
    ): JsonElement {
        if (tokens.isEmpty()) {
            return JsonPrimitive(value)
        }

        val mutableRoot = GsonUtil.parseString(GsonUtil.gson.toJson(root))
        var current: JsonElement = mutableRoot

        for (i in 0 until tokens.size - 1) {
            val token = tokens[i]
            current = when (token) {
                is JsonPathToken.Segment -> {
                    (current as? JsonObject)?.get(token.name) ?: return mutableRoot
                }
                is JsonPathToken.Idx -> {
                    (current as? JsonArray)?.get(token.i) ?: return mutableRoot
                }
            }
        }

        val leafToken = tokens.last()
        when (leafToken) {
            is JsonPathToken.Segment -> {
                (current as? JsonObject)?.addProperty(leafToken.name, value)
            }
            is JsonPathToken.Idx -> {
                (current as? JsonArray)?.set(leafToken.i, JsonPrimitive(value))
            }
        }

        return mutableRoot
    }
}