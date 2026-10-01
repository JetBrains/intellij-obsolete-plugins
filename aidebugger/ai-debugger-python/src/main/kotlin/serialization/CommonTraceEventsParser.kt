package com.intellij.aidebugger.python.serialization

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.intellij.aidebugger.common.models.entities.SerializableTraceEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object CommonTraceEventsParser {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(SerializableTraceEvent::class.java, CommonTraceEventDeserializer())
        .create()

    fun parse(text: String): SerializableTraceEvent {
        val event = gson.fromJson(
            JsonParser.parseString(text),
            SerializableTraceEvent::class.java
        )

        return event
    }
}

fun Flow<String>.asSerializableTraceEvents() = map {
    CommonTraceEventsParser.parse(it)
}
