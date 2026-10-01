package com.intellij.aidebugger.koog.session

import ai.koog.agents.core.feature.message.FeatureEvent
import ai.koog.agents.core.feature.message.FeatureMessage
import ai.koog.agents.core.feature.model.events.AgentClosingEvent
import ai.koog.agents.core.feature.model.events.AgentCompletedEvent
import ai.koog.agents.core.feature.model.events.AgentExecutionFailedEvent
import ai.koog.agents.core.feature.model.events.AgentStartingEvent
import ai.koog.agents.core.feature.model.events.DefinedFeatureEvent
import ai.koog.agents.core.feature.model.events.GraphStrategyStartingEvent
import ai.koog.agents.core.feature.model.events.LLMCallCompletedEvent
import ai.koog.agents.core.feature.model.events.LLMCallStartingEvent
import ai.koog.agents.core.feature.model.events.NodeExecutionCompletedEvent
import ai.koog.agents.core.feature.model.events.NodeExecutionFailedEvent
import ai.koog.agents.core.feature.model.events.NodeExecutionStartingEvent
import ai.koog.agents.core.feature.model.events.StrategyCompletedEvent
import ai.koog.agents.core.feature.model.events.StrategyEventGraphNode
import ai.koog.agents.core.feature.model.events.StrategyStartingEvent
import ai.koog.agents.core.feature.model.events.ToolCallCompletedEvent
import ai.koog.agents.core.feature.model.events.ToolCallFailedEvent
import ai.koog.agents.core.feature.model.events.ToolCallStartingEvent
import ai.koog.agents.core.feature.model.events.ToolValidationFailedEvent
import com.intellij.aidebugger.common.models.RequirementsNotMetInfo
import com.intellij.aidebugger.common.models.TraceEventsRepositoryBase
import com.intellij.aidebugger.common.models.entities.PayloadKey
import com.intellij.aidebugger.common.models.entities.SimpleGraph
import com.intellij.aidebugger.common.models.entities.SimpleGraphEdge
import com.intellij.aidebugger.common.models.entities.SimpleGraphNode
import com.intellij.aidebugger.koog.AiDebuggerKoogBundle
import com.intellij.aidebugger.koog.execution.KoogLibDependencyDetector
import com.intellij.aidebugger.koog.model.AgentStartingTraceEvent
import com.intellij.aidebugger.koog.model.LLMCallStartingTraceEvent
import com.intellij.aidebugger.koog.model.NodeExecutionStartingTraceEvent
import com.intellij.aidebugger.koog.model.StrategyStartingTraceEvent
import com.intellij.aidebugger.koog.model.ToolExecutionStartingTraceEvent
import com.intellij.aidebugger.koog.model.toTraceEvent
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

class KoogTraceEventsRepository(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
    private val transport: KoogDebuggerTransport,
) : TraceEventsRepositoryBase() {

    companion object {
        private val logger = thisLogger()

        // System node names in Koog that are used inside a graph to indicate start and finish points.
        private const val START_NODE_NAME = "__start__"
        private const val FINISH_NODE_NAME = "__finish__"
    }

    // TODO: This is a hacky approach to get current active agent/strategy/node for koog agent execution.
    //  This approach might not correctly work for parallel nodes execution.
    //  Need to update this with receiving metadata from Koog events directly.
    private var _currentAgentId: String? = null
    private var _currentStrategy: String? = null
    private var _currentNodeName: String? = null
    private var _currentNodeInput: JsonElement? = null

    private val _isFinished: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val finished: StateFlow<Boolean>
        get() = _isFinished

    private val _requirementsNotMet: MutableStateFlow<RequirementsNotMetInfo> = MutableStateFlow(
        RequirementsNotMetInfo(isNotMet = false)
    )
    override val requirementsNotMet: StateFlow<RequirementsNotMetInfo> =
        _requirementsNotMet

    fun startEventsProcessing() {
        logger.debug { "Start processing events in Koog events repository" }

        // Check Koog library dependency
        coroutineScope.launch {
            KoogLibDependencyDetector.getInstance(project).isValidKoogLibrary.collect { isValid ->
                // Update the requirementNotMet property when the Koog version has been changed
                // when AI Debugger session and repository are already created.
                _requirementsNotMet.value = RequirementsNotMetInfo(
                    isNotMet = !isValid,
                    message =
                        if (!isValid) { AiDebuggerKoogBundle.message("aitoolkit.feed.outdatedKoog.text") }
                        else { null },
                )
            }
        }

        val collectEventsJob = coroutineScope.launch {
            transport.incoming.receiveAsFlow().collect { message ->
                logger.trace { "Process Koog message: $message" }
                dispatchEvent(message)
            }
        }

        collectEventsJob.invokeOnCompletion {
            logger.debug { "Events processing completed in Koog events repository" }
            _isFinished.value = true
        }
    }

    //region Private Methods

    /**
     * The 'runId' property is used as a group id. It will combine all events inside one run.
     */
    private suspend fun dispatchEvent(message: FeatureMessage) {
        logger.trace { "Received message from Koog agent: $message" }

        when (message) {
            is AgentStartingEvent -> {
                _currentAgentId = message.agentId
                addEventToState(traceEvent = message.toTraceEvent(null))
            }

            is AgentCompletedEvent -> {
                // Get agent start event id and update the event
                val agentStartEventId = AgentStartingTraceEvent.createId(message.runId, message.agentId)

                updateEventInState(eventId = agentStartEventId) { agentStartEvent ->
                    message.toTraceEvent(startEvent = agentStartEvent)
                }
                _currentAgentId = null
            }

            is AgentClosingEvent -> {
                // Agent close event indicates that a server will be closed as well.
                // Close current transport and start a reconnection logic after a small delay
                // to process further connections if there are any inside a running process
                // (support the case when two Koog agents are executed one after another).
                logger.debug { "Received agent closing event. Terminate current Koog debugger transport and start reconnection process (transport: (${transport.name})" }
                transport.reconnect()
            }

            is AgentExecutionFailedEvent -> {
                // Get agent start event id and update the event
                val agentStartEventId = AgentStartingTraceEvent.createId(message.runId, message.agentId)

                updateEventInState(eventId = agentStartEventId) { agentStartEvent ->
                    message.toTraceEvent(startEvent = agentStartEvent)
                }
                _currentAgentId = null
            }

            is StrategyStartingEvent -> {
                _currentStrategy = message.strategyName

                val currentAgentId = _currentAgentId ?: error("Unable to get current agent id info")
                val parentEventId = AgentStartingTraceEvent.createId(runId = message.runId, agentId = currentAgentId)
                addEventToState(traceEvent = message.toTraceEvent(parentId = parentEventId))

                // Update the agent start event with the graph if present
                val graph = (message as? GraphStrategyStartingEvent)?.let { graphStrategyStartEvent ->
                    val simpleGraphNodes = graphStrategyStartEvent.graph.nodes
                        .map { node -> SimpleGraphNode(node.id, node.graphNodeType(), null) }

                    val simpleGraphEdges = graphStrategyStartEvent.graph.edges
                        .map { edge -> SimpleGraphEdge(edge.sourceNode.id, edge.targetNode.id) }

                    SimpleGraph(simpleGraphNodes, simpleGraphEdges)
                }

                val agentStartEventId =
                    AgentStartingTraceEvent.createId(runId = message.runId, agentId = currentAgentId)

                updateEventInState(agentStartEventId) { agentStartEvent ->
                    agentStartEvent.copy(
                        payload =
                            buildMap {
                                putAll(agentStartEvent.payload)
                                graph?.let { graph -> put(PayloadKey.Graph, graph) }
                            }
                    )
                }
            }

            is StrategyCompletedEvent -> {
                // Get strategy start event id and update the event
                val strategyStartEventId = StrategyStartingTraceEvent.createId(message.runId, message.strategyName)

                updateEventInState(eventId = strategyStartEventId) { strategyStartEvent ->
                    message.toTraceEvent(startEvent = strategyStartEvent)
                }
                _currentStrategy = null
            }

            is NodeExecutionStartingEvent -> {
                _currentNodeName = message.nodeName
                _currentNodeInput = message.input

                val currentStrategyName = _currentStrategy ?: error("Unable to get current strategy name info")
                val parentEventId =
                    StrategyStartingTraceEvent.createId(runId = message.runId, strategyName = currentStrategyName)

                val traceEvent = message.toTraceEvent(parentId = parentEventId)
                addEventToState(traceEvent = traceEvent)

                // TODO: Temporary hack to update a strategy start trace event with a user input data.
                //  Should be fixed when input appears in Koog's [AgentStartingEvent] and [StrategyStartingEvent].
                if (message.nodeName == START_NODE_NAME) {
                    val strategyStartEventId = StrategyStartingTraceEvent.createId(message.runId, currentStrategyName)
                    updateEventInState(eventId = strategyStartEventId) { strategyStartEvent ->
                        strategyStartEvent.copy(
                            payload =
                                buildMap {
                                    putAll(strategyStartEvent.payload)
                                    put(PayloadKey.Inputs, message.input)
                                }
                        )
                    }
                }
            }

            is NodeExecutionCompletedEvent -> {
                // Get node execution start event id and update the event
                val nodeExecutionStartEventId =
                    NodeExecutionStartingTraceEvent.createId(message.runId, message.nodeName, message.input.toString())

                updateEventInState(eventId = nodeExecutionStartEventId) { nodeExecutionStartEvent ->
                    message.toTraceEvent(startEvent = nodeExecutionStartEvent)
                }
                _currentNodeName = null
            }

            is NodeExecutionFailedEvent -> {
                // Get node execution start event id and update the event
                val nodeExecutionStartEventId =
                    NodeExecutionStartingTraceEvent.createId(message.runId, message.nodeName, message.input.toString())

                updateEventInState(eventId = nodeExecutionStartEventId) { nodeExecutionStartEvent ->
                    message.toTraceEvent(startEvent = nodeExecutionStartEvent)
                }
                _currentNodeName = null
            }

            is LLMCallStartingEvent -> {
                val currentNodeName = _currentNodeName ?: error("Unable to get current node name info")
                val currentNodeInput = _currentNodeInput ?: error("Unable to get current node input info")

                val parentEventId = NodeExecutionStartingTraceEvent.createId(
                    runId = message.runId,
                    nodeName = currentNodeName,
                    nodeInput = currentNodeInput.toString()
                )

                addEventToState(traceEvent = message.toTraceEvent(parentId = parentEventId))
            }

            is LLMCallCompletedEvent -> {
                val inputMessageContent = message.prompt.messages.lastOrNull()?.content ?: ""

                // Get node execution start event id and update the event
                val beforeLLMCallEventId =
                    LLMCallStartingTraceEvent.createId(message.runId, content = inputMessageContent)

                updateEventInState(eventId = beforeLLMCallEventId) { beforeLLMCallEvent ->
                    message.toTraceEvent(startEvent = beforeLLMCallEvent)
                }
            }

            is ToolCallStartingEvent -> {
                val currentNodeName = _currentNodeName ?: error("Unable to get current node name info")
                val currentNodeInput = _currentNodeInput ?: error("Unable to get current node input info")

                val parentEventId = NodeExecutionStartingTraceEvent.createId(
                    runId = message.runId,
                    nodeName = currentNodeName,
                    nodeInput = currentNodeInput.toString()
                )

                val traceEvent = message.toTraceEvent(parentId = parentEventId)
                addEventToState(traceEvent = traceEvent)
            }

            is ToolCallCompletedEvent -> {
                // Get node execution start event id and update the event
                val toolCallEventId =
                    ToolExecutionStartingTraceEvent.createId(message.runId, toolCallId = message.toolCallId.toString())

                updateEventInState(eventId = toolCallEventId) { toolCallEvent ->
                    message.toTraceEvent(startEvent = toolCallEvent)
                }
            }

            is ToolCallFailedEvent -> {
                // Get node execution start event id and update the event
                val toolCallEventId =
                    ToolExecutionStartingTraceEvent.createId(message.runId, toolCallId = message.toolCallId.toString())

                updateEventInState(eventId = toolCallEventId) { toolCallEvent ->
                    message.toTraceEvent(startEvent = toolCallEvent)
                }
            }

            is ToolValidationFailedEvent -> {
                // Get node execution start event id and update the event
                val toolCallEventId =
                    ToolExecutionStartingTraceEvent.createId(message.runId, toolCallId = message.toolCallId.toString())

                updateEventInState(eventId = toolCallEventId) { toolCallEvent ->
                    message.toTraceEvent(startEvent = toolCallEvent)
                }
            }

            is DefinedFeatureEvent,
            is FeatureEvent -> {
                logger.warn("Event type '${message::class.simpleName}' is not supported yet")
            }
        }
    }

    private fun StrategyEventGraphNode.graphNodeType(): String =
        if (this.id == START_NODE_NAME || this.id == FINISH_NODE_NAME) {
            "schema"
        }
        else {
            "runnable"
        }

    //endregion Private Methods
}
