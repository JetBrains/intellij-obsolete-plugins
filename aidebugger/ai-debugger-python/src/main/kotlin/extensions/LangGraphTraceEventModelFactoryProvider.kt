package com.intellij.aidebugger.python.extensions

import com.intellij.aidebugger.common.extensionPoints.TraceEventViewModelFactoryProvider
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.models.entities.PayloadKey
import com.intellij.aidebugger.common.models.entities.TraceEvent
import com.intellij.aidebugger.common.models.entities.getPayloadOr
import com.intellij.aidebugger.common.viewModels.InputContentViewVM
import com.intellij.aidebugger.common.viewModels.OutputContentViewVM
import com.intellij.aidebugger.common.viewModels.ToolArgumentVM
import com.intellij.aidebugger.common.viewModels.ToolCallVM
import com.intellij.aidebugger.common.viewModels.TraceEventViewModelFactory
import com.intellij.aidebugger.common.viewModels.ViewModelBase
import com.intellij.aidebugger.common.viewModels.ViewWithToolCallsVM
import com.intellij.aidebugger.common.viewModels.dataToVM
import com.intellij.aidebugger.python.viewModels.getLastMessage

class LangGraphTraceEventModelFactoryProvider : TraceEventViewModelFactoryProvider {

    override fun getViewModelFactories(): List<TraceEventViewModelFactory> = listOf(
        LangGraphInputContentViewFactory(),
        LangGraphOutputContentViewFactory(),
        LangGraphToolCallsFactory(),
    )
}

// TODO remove
class LangGraphDataVM: TraceEventViewModelFactory {
    override fun supportedFrameworks(): Set<Framework> = setOf(Framework.LangGraph)

    override fun tryCreateView(event: TraceEvent): ViewModelBase? {
        val inputs = event.getPayloadOr<Any?>(PayloadKey.Inputs, null) ?: return null
        return dataToVM(inputs)
    }
}

class LangGraphInputContentViewFactory : TraceEventViewModelFactory {
    override fun supportedFrameworks(): Set<Framework> = setOf(Framework.LangGraph)

    override fun tryCreateView(event: TraceEvent): ViewModelBase? {
        val inputs = event.getPayloadOr<Any?>(PayloadKey.Inputs, null) ?: return null
        val text = getTextContent(inputs) ?: return null

        return InputContentViewVM(text)
    }
}

class LangGraphOutputContentViewFactory : TraceEventViewModelFactory {
    override fun supportedFrameworks(): Set<Framework> = setOf(Framework.LangGraph)

    override fun tryCreateView(event: TraceEvent): ViewModelBase? {
        val outputs = event.getPayloadOr<Any?>(PayloadKey.Outputs, null) ?: return null
        val text = getTextContent(outputs) ?: return null

        return OutputContentViewVM(text)
    }
}

class LangGraphToolCallsFactory : TraceEventViewModelFactory {
    override fun supportedFrameworks(): Set<Framework> = setOf(Framework.LangGraph)

    override fun tryCreateView(event: TraceEvent): ViewModelBase? {
        val outputs = event.getPayloadOr<Any?>(PayloadKey.Outputs, null) ?: return null
        val lastMessage = getLastMessage(outputs) as? Map<*, *> ?: return null

        val toolCalls = getToolCallsFromMessage(lastMessage)

        if (toolCalls.isEmpty()) return null

        return ViewWithToolCallsVM(toolCalls)
    }
}

fun getToolCallsFromMessage(message: Map<*, *>): List<ToolCallVM> = ((message["tool_calls"] as? List<*>)
    ?.mapNotNull { toolCall -> (toolCall as? Map<*, *>)?.let { toolCall -> parseToolCall(toolCall) } }
    ?: listOf())

fun getTextContent(data: Any): String?  {
    val lastMessage = getLastMessage(data) ?: return null

    return getTextContentFromMessage(lastMessage)
}

fun getTextContentFromMessage(message: Any): String? {
    return when (message) {
        is Map<*, *> -> {
            val content = message["content"] as? String ?: return null
            if (content.isEmpty()) return null
            content
        }

        is String -> {
            message
        }

        else -> null
    }
}

fun getStructuredContent(data: Any): Any? {
    return when (val lastMessage = getLastMessage(data)) {
        is Map<*, *>,
        is List<*> -> dataToVM(lastMessage)
        else -> null
    }
}

fun parseToolCall(toolCall: Map<*, *>): ToolCallVM? {
    val name = toolCall["name"] as? String ?: return null
    val argumentVms = (toolCall["args"] as? Map<*, *> ?: emptyMap<String, Any>())
        .map { (key, value) -> ToolArgumentVM(key.toString(), value.toString()) }
        .toList()

    return ToolCallVM(name, argumentVms)
}