package com.intellij.aidebugger.common.utility

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser

object GsonUtil {
    val gson: Gson = Gson()
    val prettyGson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun parseString(json: String): JsonElement = JsonParser.parseString(json)

    fun toJsonElement(value: Any?): JsonElement = gson.toJsonTree(value)
}