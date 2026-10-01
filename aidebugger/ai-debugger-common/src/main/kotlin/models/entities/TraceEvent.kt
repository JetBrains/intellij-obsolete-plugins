package com.intellij.aidebugger.common.models.entities

import kotlinx.collections.immutable.PersistentSet
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

enum class EventType {
    General,
    Group,
    Init,
    LlmCall,
    ToolCall,
    Exception,
}

object PayloadKey {
    val Inputs: String = "inputs"
    val Outputs: String = "outputs"
    val StackTrace: String = "stack_trace"
    val Graph: String = "graph"
    val Exception: String = "exception"
    val Messages: String = "messages"
    val Model: String = "model"
}

val EmptyStackTrace: List<EventStackFrame> = emptyList<EventStackFrame>()

@Serializable
data class TraceEvent(
    val id: String,
    val parentId: String?,
    val childIds: PersistentSet<String>,
    val name: String,
    val type: EventType,
    val framework: Framework,
    val timestampStartMs: Long,
    val timestampEndMs: Long,
    val finished: Boolean,
    val payload: Map<String, @Contextual Any?>
)

inline fun <reified T> TraceEvent.getPayloadOr(key: String, default: T): T {
    val value = payload[key] ?: return default
    return if (value is T) value else default
}
