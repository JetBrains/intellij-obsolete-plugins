package com.intellij.aidebugger.common.views.utility

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.intellij.aidebugger.common.views.JsonFormatterTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull


fun annotateJson(jsonString: String, theme: JsonFormatterTheme): AnnotatedString? {
    val parsed: JsonElement = run {
        try { Json.parseToJsonElement(jsonString) } catch (_: Throwable) { null }
    } ?: return null

    return buildAnnotatedString {
        fun punct(s: String) = withStyle(SpanStyle(color = theme.punctColor)) { append(s) }
        fun nl() = append("\n")
        fun ind(level: Int) = append("  ".repeat(level))

        fun emitValue(v: JsonElement, level: Int) {
            when (v) {
                is JsonObject -> {
                    punct("{"); if (v.isEmpty()) { punct(")"); return } // quick exit handled below
                    nl()
                    val entries = v.entries.toList()
                    entries.forEachIndexed { i, (k, value) ->
                        ind(level + 1)
                        // "key":
                        withStyle(SpanStyle(color = theme.keyColor)) { append("\"$k\"") }
                        punct(": "); emitValue(value, level + 1)
                        if (i != entries.lastIndex) punct(",")
                        nl()
                    }
                    ind(level); punct("}")
                }
                is JsonArray -> {
                    punct("[")
                    if (v.isEmpty()) { punct("]"); return }
                    nl()
                    v.forEachIndexed { i, el ->
                        ind(level + 1)
                        emitValue(el, level + 1)
                        if (i != v.lastIndex) punct(",")
                        nl()
                    }
                    ind(level); punct("]")
                }
                is JsonPrimitive -> {
                    when {
                        v.isString -> withStyle(SpanStyle(color = theme.stringColor)) {
                            append("\"${v.content}\"")
                        }
                        v.booleanOrNull != null -> withStyle(SpanStyle(color = theme.boolColor)) {
                            append(v.boolean.toString())
                        }
                        v.longOrNull != null || v.doubleOrNull != null -> withStyle(SpanStyle(color = theme.numberColor)) {
                            append(v.content)
                        }
                        v.content.equals("null", ignoreCase = true) -> withStyle(SpanStyle(color = theme.nullColor)) {
                            append("null")
                        }
                        else -> append(v.content)
                    }
                }
            }
        }

        emitValue(parsed, level = 0)
    }
}