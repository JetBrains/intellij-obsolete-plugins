package com.intellij.aidebugger.koog.model

import ai.koog.agents.core.feature.message.FeatureMessage
import ai.koog.agents.core.feature.model.events.AIAgentFinishedEvent
import ai.koog.agents.core.feature.model.events.AIAgentStartedEvent
import ai.koog.agents.core.feature.model.events.AIAgentStrategyFinishedEvent
import ai.koog.agents.core.feature.model.events.AIAgentStrategyStartEvent
import ai.koog.agents.core.feature.model.events.AgentClosingEvent
import ai.koog.agents.core.feature.model.events.AgentCompletedEvent
import ai.koog.agents.core.feature.model.events.AgentExecutionFailedEvent
import ai.koog.agents.core.feature.model.events.AgentStartingEvent
import ai.koog.agents.core.feature.model.events.LLMCallCompletedEvent
import ai.koog.agents.core.feature.model.events.LLMCallStartingEvent
import ai.koog.agents.core.feature.model.events.NodeExecutionCompletedEvent
import ai.koog.agents.core.feature.model.events.NodeExecutionFailedEvent
import ai.koog.agents.core.feature.model.events.NodeExecutionStartingEvent
import ai.koog.agents.core.feature.model.events.StrategyCompletedEvent
import ai.koog.agents.core.feature.model.events.StrategyStartingEvent
import ai.koog.agents.core.feature.model.events.ToolCallCompletedEvent
import ai.koog.agents.core.feature.model.events.ToolCallFailedEvent
import ai.koog.agents.core.feature.model.events.ToolCallStartingEvent
import ai.koog.agents.core.feature.model.events.ToolValidationFailedEvent
import ai.koog.prompt.message.Message
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.models.entities.PayloadKey
import com.intellij.aidebugger.common.models.entities.SimpleGraph
import com.intellij.aidebugger.common.models.entities.TraceEvent
import com.intellij.aidebugger.koog.AiDebuggerKoogBundle
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Converts a `FeatureMessage` instance to a corresponding `TraceEvent`.
 *
 * The method converts Koog 'starting' events, like [AIAgentStartedEvent], [AIAgentStrategyStartEvent], etc. that
 * creates an initial trace event for AI Debugger plugin and add this event into a state.
 *
 * For processing closing events, please refer to the [toTraceEvent(startEvent: TraceEvent)] method.
 *
 * @param parentId The optional parent ID associated with the `TraceEvent`. This is required for certain event types
 *                (e.g., strategy events, node execution events, LLM call events, and tool call events). If the parent
 *                ID is null when required, an exception will be thrown.
 */
fun FeatureMessage.toTraceEvent(parentId: String?): TraceEvent {

    val traceEvent = when (this) {
        is AgentStartingEvent -> {
            AgentStartingTraceEvent.create(
                runId = this.runId,
                agentId = this.agentId,
                timestampStartMs = this.timestamp
            )
        }

        is AgentClosingEvent -> {
            error("Event is not supported by AI Debugger platform: ${this.javaClass.simpleName}")
        }

        is StrategyStartingEvent -> {
            requireNotNull(parentId) { "Parent id is null for event: ${this.javaClass.simpleName}" }

            StrategyStartingTraceEvent.create(
                parentId = parentId,
                runId = this.runId,
                strategyName = this.strategyName,
                timestampStartMs = this.timestamp,
            )
        }

        is NodeExecutionStartingEvent -> {
            requireNotNull(parentId) { "Parent id is null for event: ${this.javaClass.simpleName}" }

            NodeExecutionStartingTraceEvent.create(
                parentId = parentId,
                runId = this.runId,
                nodeName = this.nodeName,
                timestampStartMs = this.timestamp,
                nodeInput = this.input,
            )
        }

        is LLMCallStartingEvent -> {
            requireNotNull(parentId) { "Parent id is null for event: ${this.javaClass.simpleName}" }

            LLMCallStartingTraceEvent.create(
                parentId = parentId,
                runId = this.runId,
                timestampStartMs = this.timestamp,
                messages = this.prompt.messages
            )
        }

        is ToolCallStartingEvent -> {
            requireNotNull(parentId) { "Parent id is null for event: ${this.javaClass.simpleName}" }

            ToolExecutionStartingTraceEvent.create(
                parentId = parentId,
                runId = this.runId,
                toolCallId = this.toolCallId.toString(),
                timestampStartMs = this.timestamp,
                toolName = this.toolName,
                toolArgs = this.toolArgs
            )
        }

        is AgentCompletedEvent,
        is AgentExecutionFailedEvent,
        is StrategyCompletedEvent,
        is NodeExecutionCompletedEvent,
        is NodeExecutionFailedEvent,
        is LLMCallCompletedEvent,
        is ToolCallCompletedEvent,
        is ToolCallFailedEvent,
        is ToolValidationFailedEvent ->  {
            error("Event ${javaClass.simpleName} is a finish event. Please use 'toTraceEvent(startEvent: TraceEvent)' method instead.")
        }

        else -> {
            error("Unsupported event type: $this")
        }
    }

    return traceEvent
}

/**
 * Converts a FeatureMessage instance into a corresponding TraceEvent.
 *
 * The method converts Koog 'finish' events, like [AIAgentFinishedEvent], [AIAgentStrategyFinishedEvent], etc. that
 * are used to update an already exising trace event in AI Debugger state.
 *
 * For processing starting events, please refer to the [toTraceEvent(parentId: String?)] method.
 *
 * @param startEvent The TraceEvent representing the start event to be
 *                   associated with this FeatureMessage.
 */
fun FeatureMessage.toTraceEvent(startEvent: TraceEvent): TraceEvent {
    val traceEvent = when (this) {
        is AgentCompletedEvent -> {
            AgentCompletedTraceEvent.create(
                agentStartEvent = startEvent,
                timestampFinishMs = this.timestamp,
                result = this.result
            )
        }

        is AgentExecutionFailedEvent -> {
            AgentExecutionFailedTraceEvent.create(
                agentStartEvent = startEvent,
                timestampFinishMs = this.timestamp,
                error = this.error?.message.toString(),
                stackTrace = this.error?.stackTrace.toString()
            )
        }

        is AgentClosingEvent -> {
            error("Event is not supported by AI Debugger platform: ${this.javaClass.simpleName}")
        }

        is StrategyCompletedEvent -> {
            StrategyCompletedTraceEvent.create(
                strategyStartEvent = startEvent,
                timestampFinishMs = this.timestamp,
                result = this.result
            )
        }

        is NodeExecutionCompletedEvent -> {
            NodeExecutionCompletedTraceEvent.create(
                nodeStartEvent = startEvent,
                timestampFinishMs = this.timestamp,
                nodeOutput = this.output
            )
        }

        is NodeExecutionFailedEvent -> {
            NodeExecutionFailedTraceEvent.create(
                nodeStartEvent = startEvent,
                timestampFinishMs = this.timestamp,
                error = this.error.message,
                stackTrace = this.error.stackTrace
            )
        }

        is LLMCallCompletedEvent -> {
            LLMCallCompletedTraceEvent.create(
                llmCallStartEvent = startEvent,
                timestampFinishMs = this.timestamp,
                responses = this.responses
            )
        }

        is ToolCallCompletedEvent -> {
            ToolExecutionCompletedTraceEvent.create(
                toolCallStartEvent = startEvent,
                timestampFinishMs = this.timestamp,
                result = this.result.toString()
            )
        }

        is ToolCallFailedEvent -> {
            ToolExecutionFailedTraceEvent.create(
                toolCallStartEvent = startEvent,
                timestampFinishMs = this.timestamp,
                error = this.error?.message.toString(),
                stackTrace = this.error?.stackTrace.toString()
            )
        }
        is ToolValidationFailedEvent -> {
            ToolValidationFailedTraceEvent.create(
                toolCallStartEvent = startEvent,
                timestampFinishMs = this.timestamp,
                error = this.error.toString()
            )
        }

        is AgentStartingEvent,
        is StrategyStartingEvent,
        is NodeExecutionStartingEvent,
        is LLMCallStartingEvent,
        is ToolCallStartingEvent -> {
            error("Event ${javaClass.simpleName} is a start event. Please use 'toTraceEvent(parentId: String)' method instead.")
        }

        else -> {
            error("Unsupported event type: $this")
        }
    }

    return traceEvent
}

class AgentStartingTraceEvent {

    companion object {
        fun createId(runId: String, agentId: String): String = "koog_agent_run.${runId}.agent.${agentId}"

        fun create(
            runId: String,
            agentId: String,
            timestampStartMs: Long,
            graph: SimpleGraph? = null
        ): TraceEvent =
            TraceEvent(
                id = createId(runId, agentId),
                parentId = null,
                childIds = persistentSetOf(),
                name = AiDebuggerKoogBundle.message("aitoolkit.debugger.koog.agent.trace.event.agent.name", agentId),
                type = EventType.General,
                framework = Framework.Koog,
                timestampStartMs = timestampStartMs,
                timestampEndMs = timestampStartMs,
                finished = false,
                payload = buildMap {
                    graph?.let { graph -> put(PayloadKey.Graph, graph) }
                }
            )
    }
}

class AgentCompletedTraceEvent {

    companion object {
        fun create(
            agentStartEvent: TraceEvent,
            timestampFinishMs: Long,
            result: String?
        ): TraceEvent =
            agentStartEvent.copy(
                finished = true,
                timestampEndMs = timestampFinishMs,
                payload = agentStartEvent.payload + (
                    mapOf(
                        PayloadKey.Outputs to result
                    )
                )
            )
    }
}

class AgentExecutionFailedTraceEvent {
    companion object {
        fun create(
            agentStartEvent: TraceEvent,
            timestampFinishMs: Long,
            error: String,
            stackTrace: String
        ): TraceEvent =
            agentStartEvent.copy(
                finished = true,
                timestampEndMs = timestampFinishMs,
                payload = agentStartEvent.payload + (
                    mapOf(
                        PayloadKey.Exception to error,
                        PayloadKey.StackTrace to stackTrace
                    )
                )
            )
    }
}

class StrategyStartingTraceEvent {
    companion object {
        fun createId(runId: String, strategyName: String): String = "koog_agent_run.${runId}.strategy.${strategyName}"

        fun create(
            parentId: String,
            runId: String,
            strategyName: String,
            timestampStartMs: Long,
        ): TraceEvent =
            TraceEvent(
                id = createId(runId, strategyName),
                parentId = parentId,
                childIds = persistentSetOf(),
                name = AiDebuggerKoogBundle.message("aitoolkit.debugger.koog.agent.trace.event.strategy.name", strategyName),
                type = EventType.General,
                framework = Framework.Koog,
                timestampStartMs = timestampStartMs,
                timestampEndMs = timestampStartMs,
                finished = false,
                payload = mapOf()
            )
    }
}

class StrategyCompletedTraceEvent {
    companion object {
        fun create(
            strategyStartEvent: TraceEvent,
            timestampFinishMs: Long,
            result: String?
        ): TraceEvent =
            strategyStartEvent.copy(
                finished = true,
                timestampEndMs = timestampFinishMs,
                payload = strategyStartEvent.payload + (
                    mapOf(
                        PayloadKey.Outputs to result
                    )
                )
            )
    }
}

class NodeExecutionStartingTraceEvent {
    companion object {
        fun createId(runId: String, nodeName: String, nodeInput: String): String = "koog_agent_run.${runId}.node.${nodeName}.id.${nodeInput}"

        fun create(
            parentId: String,
            runId: String,
            nodeName: String,
            timestampStartMs: Long,
            nodeInput: JsonElement?,
        ): TraceEvent {
            val eventId = createId(runId, nodeName, nodeInput.toString())
            return TraceEvent(
                id = eventId,
                parentId = parentId,
                childIds = persistentSetOf(),
                name = AiDebuggerKoogBundle.message("aitoolkit.debugger.koog.agent.trace.event.node.name", nodeName),
                type = EventType.General,
                framework = Framework.Koog,
                timestampStartMs = timestampStartMs,
                timestampEndMs = timestampStartMs,
                finished = false,
                payload = mapOf(
                    PayloadKey.Inputs to nodeInput,
                )
            )
        }
    }
}

class NodeExecutionCompletedTraceEvent {
    companion object {
        fun create(
            nodeStartEvent: TraceEvent,
            timestampFinishMs: Long,
            nodeOutput: JsonElement?
        ): TraceEvent =
            nodeStartEvent.copy(
                finished = true,
                timestampEndMs = timestampFinishMs,
                payload = nodeStartEvent.payload + (
                    mapOf(
                        PayloadKey.Outputs to nodeOutput,
                    )
                )
            )
    }
}

class NodeExecutionFailedTraceEvent {
    companion object {
        fun create(
            nodeStartEvent: TraceEvent,
            timestampFinishMs: Long,
            error: String,
            stackTrace: String
        ): TraceEvent =
            nodeStartEvent.copy(
                finished = true,
                timestampEndMs = timestampFinishMs,
                payload = nodeStartEvent.payload + (
                    mapOf(
                        PayloadKey.Exception to error,
                        PayloadKey.StackTrace to stackTrace
                    )
                )
            )
    }
}

class LLMCallStartingTraceEvent {
    companion object {
        fun createId(runId: String, content: String): String = "koog_agent_run.${runId}.llm_call.${content}"

        fun create(
            parentId: String,
            runId: String,
            timestampStartMs: Long,
            messages: List<Message>
        ): TraceEvent {
            val content = messages.lastOrNull()?.content ?: ""
            return TraceEvent(
                id = createId(runId, content),
                parentId = parentId,
                childIds = persistentSetOf(),
                name = AiDebuggerKoogBundle.message("aitoolkit.debugger.koog.agent.trace.event.llm.call.name", content),
                type = EventType.LlmCall,
                framework = Framework.Koog,
                timestampStartMs = timestampStartMs,
                timestampEndMs = timestampStartMs,
                finished = false,
                payload = mapOf(
                    PayloadKey.Inputs to content,
                    PayloadKey.Messages to messages
                )
            )
        }
    }
}

class LLMCallCompletedTraceEvent {
    companion object {
        fun create(
            llmCallStartEvent: TraceEvent,
            timestampFinishMs: Long,
            responses: List<Message.Response>
        ): TraceEvent {

//            // TODO: Need to be updated when [TraceEvent] adds it's own abstractions over tokens usage data
//            val messages = responses.map { response ->
//
//                val toolCalls = mutableListOf<Map<String, Any>>()
//
//                val messagesMap = mutableMapOf<String, Any?>(
//                    "role" to response.role.name,
//                    "tool_calls" to toolCalls,
//                    "response_metadata" to buildMap {
//                        put("token_usage", buildMap {
//                            put("input_tokens", response.metaInfo.inputTokensCount?.toDouble())
//                            put("output_tokens", response.metaInfo.outputTokensCount?.toDouble())
//                            put("total_tokens", response.metaInfo.totalTokensCount?.toDouble())
//                        })
//                    }
//                )
//
//                when (response) {
//                    is Message.Assistant -> {
//                        messagesMap["content"] = response.content
//                    }
//
//                    is Message.Tool.Call -> {
//                        toolCalls.add(
//                            buildMap {
//                                put("name", response.tool)
//                                put("args", response.contentJson.toMap())
//                            }
//                        )
//                    }
//                }
//
//                messagesMap.toMap()
//            }

            val updatedPayload = mapOf(
                PayloadKey.Outputs to responses,
                PayloadKey.Messages to
                        (llmCallStartEvent.payload[PayloadKey.Messages] as? List<*>)?.let { it + responses },
            )

            val updatedEvent = llmCallStartEvent.copy(
                finished = true,
                timestampEndMs = timestampFinishMs,
                payload = llmCallStartEvent.payload + (updatedPayload)
            )

            return updatedEvent
        }
    }
}

class ToolExecutionStartingTraceEvent {
    companion object {
        fun createId(runId: String, toolCallId: String): String = "koog_agent_run.${runId}.tool_call.${toolCallId}"

        fun create(
            parentId: String,
            runId: String,
            toolCallId: String,
            timestampStartMs: Long,
            toolName: String,
            toolArgs: JsonObject,
        ): TraceEvent {
            val eventId = createId(runId, toolCallId)
            return TraceEvent(
                id = eventId,
                parentId = parentId,
                childIds = persistentSetOf(),
                name = AiDebuggerKoogBundle.message("aitoolkit.debugger.koog.agent.trace.event.tool.call.name", toolName),
                type = EventType.ToolCall,
                framework = Framework.Koog,
                timestampStartMs = timestampStartMs,
                timestampEndMs = timestampStartMs,
                finished = false,
                payload = mapOf(
                    PayloadKey.Inputs to mapOf(
                        PayloadKey.Tool to toolName,
                        PayloadKey.ToolArguments to toolArgs
                    ),
                )
            )
        }
    }
}

class ToolExecutionCompletedTraceEvent {
    companion object {
        fun create(
            toolCallStartEvent: TraceEvent,
            timestampFinishMs: Long,
            result: String
        ): TraceEvent =
            toolCallStartEvent.copy(
                finished = true,
                timestampEndMs = timestampFinishMs,
                payload = toolCallStartEvent.payload + (
                    mapOf(
                        PayloadKey.Outputs to result
                    )
                )
            )
    }
}

class ToolExecutionFailedTraceEvent {
    companion object {
        fun create(
            toolCallStartEvent: TraceEvent,
            timestampFinishMs: Long,
            error: String,
            stackTrace: String
        ): TraceEvent =
            toolCallStartEvent.copy(
                finished = true,
                timestampEndMs = timestampFinishMs,
                payload = toolCallStartEvent.payload + (
                    mapOf(
                        PayloadKey.Exception to error,
                        PayloadKey.StackTrace to stackTrace
                    )
                )
            )
    }
}

class ToolValidationFailedTraceEvent {
    companion object {
        fun create(
            toolCallStartEvent: TraceEvent,
            timestampFinishMs: Long,
            error: String
        ): TraceEvent =
            toolCallStartEvent.copy(
                finished = true,
                timestampEndMs = timestampFinishMs,
                payload = toolCallStartEvent.payload + (
                    mapOf(
                        PayloadKey.Exception to error
                    )
                )
            )
    }
}
