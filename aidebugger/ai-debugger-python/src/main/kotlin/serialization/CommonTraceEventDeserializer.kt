package com.intellij.aidebugger.python.serialization

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.intellij.aidebugger.common.models.entities.BasicSerializableTrace
import com.intellij.aidebugger.common.models.entities.EventStackFrame
import com.intellij.aidebugger.common.models.entities.InstantTrace
import com.intellij.aidebugger.common.models.entities.PayloadKey
import com.intellij.aidebugger.common.models.entities.SerializableTraceEvent
import com.intellij.aidebugger.common.models.entities.SimpleGraph
import com.intellij.aidebugger.common.models.entities.SimpleGraphEdge
import com.intellij.aidebugger.common.models.entities.SimpleGraphNode
import com.intellij.aidebugger.common.models.entities.SpanEnterTrace
import com.intellij.aidebugger.common.models.entities.SpanExitTrace
import com.intellij.aidebugger.common.models.entities.TraceEventType
import com.intellij.aidebugger.common.models.entities.TraceLlmModel
import com.intellij.aidebugger.common.models.entities.TraceLlmTool
import com.intellij.aidebugger.common.models.entities.TraceRequirementsNotMet
import com.intellij.aidebugger.common.models.entities.deserialize
import java.lang.reflect.Type

class CommonTraceEventDeserializer : JsonDeserializer<SerializableTraceEvent> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): SerializableTraceEvent {
        val jsonObject = json.asJsonObject

        val type = jsonObject.get("type")?.asString?.uppercase()

        return when (type) {
            TraceEventType.REQUIREMENTS_NOT_MET.name -> TraceRequirementsNotMet()
            TraceEventType.SPAN_ENTER.name -> {
                val event = context.deserializeBasicTrace(json)
                SpanEnterTrace(
                    event.id,
                    event.parentId,
                    event.traceId,
                    event.name,
                    event.eventType,
                    event.framework.deserialize(),
                    event.timestampMs,
                    deserializePayload(event.payload)
                )
            }
            TraceEventType.SPAN_EXIT.name -> {
                val event = context.deserializeBasicTrace(json)
                SpanExitTrace(
                    event.id,
                    event.parentId,
                    event.traceId,
                    event.name,
                    event.eventType,
                    event.framework.deserialize(),
                    event.timestampMs,
                    deserializePayload(event.payload)
                )
            }
            TraceEventType.INSTANT.name -> {
                val event = context.deserializeBasicTrace(json)
                InstantTrace(
                    event.id,
                    event.parentId,
                    event.traceId,
                    event.name,
                    event.eventType,
                    event.framework.deserialize(),
                    event.timestampMs,
                    deserializePayload(event.payload)
                )
            }

            else -> throw IllegalArgumentException("Unknown event type: ${type ?: "null"}")
        }
    }
}

fun deserializePayload(rawPayload: Map<String, Any?>): Map<String, Any?> {
    return rawPayload.mapValues { (key, value) ->
        when (key) {
            PayloadKey.Graph -> parseGraph(value)
            PayloadKey.StackTrace -> parseStackTrace(value)
            PayloadKey.Model -> parseModel(value)
            else -> value
        }
    }
}

private fun parseGraph(value: Any?): SimpleGraph? {
    return try {
        when (value) {
            is Map<*, *> -> {
                val nodesData = value["nodes"] as? List<*> ?: return null
                val edgesData = value["edges"] as? List<*> ?: return null

                val nodes = nodesData.mapNotNull { nodeData ->
                    when (nodeData) {
                        is Map<*, *> -> {
                            val id = nodeData["id"] as? String ?: return@mapNotNull null
                            val type = nodeData["type"] as? String
                            val data = nodeData["data"]
                            SimpleGraphNode(id, type, data)
                        }
                        else -> null
                    }
                }

                val edges = edgesData.mapNotNull { edgeData ->
                    when (edgeData) {
                        is Map<*, *> -> {
                            val source = edgeData["source"] as? String ?: return@mapNotNull null
                            val target = edgeData["target"] as? String ?: return@mapNotNull null
                            SimpleGraphEdge(source, target)
                        }
                        else -> null
                    }
                }

                SimpleGraph(nodes, edges)
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun parseStackTrace(value: Any?): List<EventStackFrame>? {
    return try {
        when (value) {
            is List<*> -> {
                value.mapNotNull { stackFrameData ->
                    when (stackFrameData) {
                        is Map<*, *> -> {
                            val filePath = stackFrameData["filePath"] as? String ?: return@mapNotNull null
                            val lineNumber = when (val lineNum = stackFrameData["lineNumber"]) {
                                is Number -> lineNum.toInt()
                                is String -> lineNum.toIntOrNull() ?: return@mapNotNull null
                                else -> return@mapNotNull null
                            }
                            val functionName = stackFrameData["functionName"] as? String ?: return@mapNotNull null

                            EventStackFrame(
                                filePath = filePath,
                                lineNumber = lineNumber,
                                functionName = functionName
                            )
                        }
                        else -> null
                    }
                }
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun parseModel(value: Any?): TraceLlmModel? {
    return try {
        when (value) {
            is Map<*, *> -> {
                val model = value["model"] as? String
                val modelName = value["model_name"] as? String ?: value["modelName"] as? String
                val temperature = when (val temp = value["temperature"]) {
                    is Number -> temp.toDouble()
                    is String -> temp.toDoubleOrNull()
                    else -> null
                }
                val toolsData = value["tools"] as? List<*> ?: emptyList<Any>()

                val tools = toolsData.mapNotNull { toolData ->
                    when (toolData) {
                        is Map<*, *> -> {
                            val name = toolData["name"] as? String ?: return@mapNotNull null
                            val description = toolData["description"] as? String ?: ""
                            TraceLlmTool(name, description)
                        }
                        else -> null
                    }
                }

                TraceLlmModel(model, modelName, temperature, tools)
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

internal fun JsonDeserializationContext.deserializeBasicTrace(json: JsonElement) =
    deserialize<BasicSerializableTrace>(
        json,
        BasicSerializableTrace::class.java
    )