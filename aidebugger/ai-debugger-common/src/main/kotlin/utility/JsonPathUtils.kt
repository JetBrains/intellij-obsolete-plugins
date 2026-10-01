package com.intellij.aidebugger.common.utility

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonNull

/**
 * Minimal JSON-path-like utilities for Gson JsonElement trees.
 * Supported syntax:
 * - $.prop1.prop2
 * - $.arr[0], negative index [-1] means last element
 * - Mixed: $.events[0][0].payload.inputs.messages[0].content
 */
object JsonPathUtils {

    fun parseJsonPath(path: String): List<JsonPathToken> {
        return JsonPathParser(path.trim()).parse()
    }

    fun getValueByPath(root: JsonElement, tokens: List<JsonPathToken>): JsonElement? {
        var current: JsonElement? = root
        for (token in tokens) {
            current = when (token) {
                is JsonPathToken.Segment -> current?.asJsonObject?.get(token.name)
                is JsonPathToken.Idx -> {
                    val array = current?.asJsonArray ?: return null
                    val size = array.size()
                    val idx = if (token.i < 0) size + token.i else token.i

                    if (idx !in 0..<size) return null
                    array.get(idx)
                }
            }
            if (current == null || current is JsonNull) return null
        }
        return current
    }

    /**
     * Convenience method: parses path and extracts value in one call.
     * Returns null if path is invalid or value not found.
     */
    fun getValueByPath(root: JsonElement, path: String): JsonElement? {
        val tokens = try {
            parseJsonPath(path)
        } catch (e: Exception) {
            return null
        }
        return getValueByPath(root, tokens)
    }

    /**
     * Converts a JsonElement to a string representation suitable for display/storage.
     * - Primitives: returns their string value
     * - Objects/Arrays: returns JSON string representation
     * - Null: returns null
     */
    fun jsonStringLike(element: JsonElement?): String? {
        if (element == null || element is JsonNull) return null
        return when {
            element.isJsonPrimitive && element.asJsonPrimitive.isString -> element.asString
            element.isJsonPrimitive -> element.toString()
            else -> Gson().toJson(element)
        }
    }

    /**
     * Extracts value from JSON using the given path and converts to string.
     * Combines path extraction and string conversion in one call.
     * Returns null if path is invalid or value not found.
     */
    fun extractAsString(root: JsonElement, path: String): String? {
        val value = getValueByPath(root, path) ?: return null
        return jsonStringLike(value)
    }
}