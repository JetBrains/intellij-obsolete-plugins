package com.intellij.aidebugger.koog.extension

import ai.koog.prompt.message.Message
import com.intellij.aidebugger.common.extensionPoints.TraceEventViewModelFactoryProvider
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.models.entities.PayloadKey
import com.intellij.aidebugger.common.models.entities.TraceEvent
import com.intellij.aidebugger.common.viewModels.InputContentViewVM
import com.intellij.aidebugger.common.viewModels.OutputContentViewVM
import com.intellij.aidebugger.common.viewModels.OutputStructuredContentViewVM
import com.intellij.aidebugger.common.viewModels.ToolArgumentVM
import com.intellij.aidebugger.common.viewModels.ToolCallVM
import com.intellij.aidebugger.common.viewModels.TraceEventViewModelFactory
import com.intellij.aidebugger.common.viewModels.ViewModelBase
import com.intellij.aidebugger.common.viewModels.ViewWithToolCallsVM
import com.intellij.aidebugger.koog.model.Tool
import com.intellij.aidebugger.koog.model.ToolArguments
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.serialization.json.JsonObject

/**
 * Provides a collection of ViewModel factories specific to trace events.
 *
 * This class implements the TraceEventViewModelFactoryProvider interface,
 * which allows the registration of multiple factories that are capable of
 * creating specific ViewModel instances for associated trace events.
 */
class KoogTraceEventModelViewFactoryProvider : TraceEventViewModelFactoryProvider {

    override fun getViewModelFactories(): List<TraceEventViewModelFactory> = listOf(
        KoogTraceEventInputViewFactory(),
        KoogTraceEventOutputViewFactory(),
    )
}

private class KoogTraceEventInputViewFactory : KoogTraceEventViewFactory() {
    override fun generalEventView(event: TraceEvent): ViewModelBase? =
        createCommonViewModel(event)

    override fun llmCallEventView(event: TraceEvent): ViewModelBase? =
        createCommonViewModel(event)

    override fun toolCallEventView(event: TraceEvent): ViewModelBase? {
        val inputsMap = event.payload[PayloadKey.Inputs] as? Map<*, *> ?: return null

        val tool = inputsMap[PayloadKey.Tool] as? String ?: return null
        val arguments = (inputsMap[PayloadKey.ToolArguments] as? JsonObject)?.toMap() ?: return null

        val argumentsVM = arguments.map { (argKey, argValue) ->
            ToolArgumentVM(argKey, argValue.toString())
        }

        return ToolCallVM(tool, argumentsVM)
    }

    override fun exceptionEventView(event: TraceEvent): ViewModelBase? =
        createCommonViewModel(event)

    //region Private Methods

    private fun createCommonViewModel(event: TraceEvent): ViewModelBase? {
        val inputs = event.payload[PayloadKey.Inputs] ?: return null
        val content = inputs as? String ?: return null
        return InputContentViewVM(content)
    }

    //endregion Private Methods
}

private class KoogTraceEventOutputViewFactory : KoogTraceEventViewFactory() {
    override fun generalEventView(event: TraceEvent): ViewModelBase? = createCommonViewModel(event)

    override fun llmCallEventView(event: TraceEvent): ViewModelBase? {
        val outputs = event.payload[PayloadKey.Outputs] as? List<*> ?: return null

        val toolCalls = mutableListOf<Message.Tool.Call>()
        val assistantMessages = mutableListOf<Message.Assistant>()

        outputs.forEach { output ->
            val response = output as? Message.Response ?: return@forEach

            when (response) {
                is Message.Assistant -> assistantMessages.add(response)
                is Message.Tool.Call -> toolCalls.add(response)
                else -> return@forEach
            }
        }

        // Tool Calls
        if (toolCalls.isNotEmpty()) {
            val toolCallsVm: List<ToolCallVM> = toolCalls.map { toolCall ->
                val argumentsVM = toolCall.contentJson.toMap().map { (argName, argValue) ->
                    ToolArgumentVM(argName, argValue.toString())
                }
                ToolCallVM(name = toolCall.tool, arguments = argumentsVM)
            }

            return ViewWithToolCallsVM(toolCallsVm)
        }

        // Assistant
        if (assistantMessages.size == 1) {
            val assistantMessage = assistantMessages.first()
            return OutputContentViewVM(assistantMessage.content)
        }

        if (assistantMessages.size > 1) {
            val assistantMessagesMap = assistantMessages.associate { assistantMessage ->
                "role" to assistantMessage.role.name
                "content" to assistantMessage.content
                "metaInfo" to buildMap {
                    assistantMessage.metaInfo.totalTokensCount?.let { put("totalTokensCount", it) }
                    assistantMessage.metaInfo.inputTokensCount?.let { put("inputTokensCount", it) }
                    assistantMessage.metaInfo.outputTokensCount?.let { put("outputTokensCount", it) }
                    assistantMessage.metaInfo.metadata?.let { put("metadata", it.toMap()) }
                }
            }

            return OutputStructuredContentViewVM(assistantMessagesMap)
        }

        return null
    }

    override fun toolCallEventView(event: TraceEvent): ViewModelBase? = createCommonViewModel(event)

    override fun exceptionEventView(event: TraceEvent): ViewModelBase? = createCommonViewModel(event)

    //region Private Methods

    private fun createCommonViewModel(event: TraceEvent): ViewModelBase? {
        // Success
        val outputs = event.payload[PayloadKey.Outputs]
        val result = outputs as? String

        if (result != null) {
            return OutputContentViewVM(result)
        }

        // Failures
        val exception = event.payload[PayloadKey.Exception] as? String ?: return null
        val stackTrace = event.payload[PayloadKey.StackTrace] as? String

        // TODO: Update to a special Error presentation VM when it will be available
        return OutputContentViewVM(exception)
    }

    //endregion Private Methods
}

private abstract class KoogTraceEventViewFactory : TraceEventViewModelFactory {

    companion object {
        private val logger = thisLogger()
    }

    override fun supportedFrameworks(): Set<Framework> {
        return setOf(Framework.Koog)
    }

    override fun tryCreateView(event: TraceEvent): ViewModelBase? {
        val view = when (event.type) {
            EventType.General -> generalEventView(event)
            EventType.LlmCall -> llmCallEventView(event)
            EventType.ToolCall -> toolCallEventView(event)
            EventType.Exception -> exceptionEventView(event)

            EventType.Init,
            EventType.Group -> null
        }

        logger.debug { "KoogTraceEventViewFactory. Defined view for event (event: ${event.id}, view: ${view?.let { it::class.simpleName }})" }
        return view
    }

    abstract fun generalEventView(event: TraceEvent): ViewModelBase?
    abstract fun llmCallEventView(event: TraceEvent): ViewModelBase?
    abstract fun toolCallEventView(event: TraceEvent): ViewModelBase?
    abstract fun exceptionEventView(event: TraceEvent): ViewModelBase?
}
