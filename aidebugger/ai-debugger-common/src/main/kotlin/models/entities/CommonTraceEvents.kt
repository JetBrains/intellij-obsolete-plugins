package com.intellij.aidebugger.common.models.entities

import com.google.gson.annotations.SerializedName

enum class TraceEventType {
    REQUIREMENTS_NOT_MET,
    SPAN_ENTER,
    SPAN_EXIT,
    INSTANT,
}

interface SerializableTraceEvent {
    val timestampMs: Long
}

interface BasicTrace: SerializableTraceEvent {
    val id: String
    val parentId: String?
    val traceId: String
    val name: String
    val eventType: EventType
    val framework: Framework
    override val timestampMs: Long
    val payload: Map<String, Any?>
}

interface SpanTrace: BasicTrace

class TraceRequirementsNotMet: SerializableTraceEvent {
    override val timestampMs: Long = System.currentTimeMillis()
}

class BasicSerializableTrace(
    @SerializedName("id")
    val id: String,

    @SerializedName("parent_id")
    val parentId: String?,

    @SerializedName("trace_id")
    val traceId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("event_type")
    val eventType: EventType,

    @SerializedName("framework")
    val framework: SerializableFramework,

    @SerializedName("timestamp_ms")
    val timestampMs: Long,

    @SerializedName("payload")
    val payload: Map<String, Any?>,
)

class SpanEnterTrace(
    override val id: String,
    override val parentId: String?,
    override val traceId: String,
    override val name: String,
    override val eventType: EventType,
    override val framework: Framework,
    override val timestampMs: Long,
    override val payload: Map<String, Any?>,
): SpanTrace

class SpanExitTrace(
    override val id: String,
    override val parentId: String?,
    override val traceId: String,
    override val name: String,
    override val eventType: EventType,
    override val framework: Framework,
    override val timestampMs: Long,
    override val payload: Map<String, Any?>,
): SpanTrace

class InstantTrace(
    override val id: String,
    override val parentId: String?,
    override val traceId: String,
    override val name: String,
    override val eventType: EventType,
    override val framework: Framework,
    override val timestampMs: Long,
    override val payload: Map<String, Any?>,
): BasicTrace


inline fun <reified T> BasicTrace.getPayloadOr(key: String, default: T): T {
    val value = payload[key] ?: return default
    return if (value is T) value else default
}

data class TraceLlmTool(
    val name: String,
    val description: String,
)

data class TraceLlmModel(
    val model: String?,
    val modelName: String?,
    val temperature: Double?,
    val tools: List<TraceLlmTool>
)