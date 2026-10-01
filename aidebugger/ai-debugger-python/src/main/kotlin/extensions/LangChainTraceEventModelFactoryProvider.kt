package com.intellij.aidebugger.python.extensions

import com.intellij.aidebugger.common.extensionPoints.TraceEventViewModelFactoryProvider
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.models.entities.PayloadKey
import com.intellij.aidebugger.common.models.entities.TraceEvent
import com.intellij.aidebugger.common.models.entities.getPayloadOr
import com.intellij.aidebugger.common.viewModels.InputContentViewVM
import com.intellij.aidebugger.common.viewModels.InputStructuredContentViewVM
import com.intellij.aidebugger.common.viewModels.OutputContentViewVM
import com.intellij.aidebugger.common.viewModels.OutputStructuredContentViewVM
import com.intellij.aidebugger.common.viewModels.ToolArgumentVM
import com.intellij.aidebugger.common.viewModels.ToolCallVM
import com.intellij.aidebugger.common.viewModels.TraceEventViewModelFactory
import com.intellij.aidebugger.common.viewModels.ViewModelBase
import com.intellij.aidebugger.common.viewModels.ViewWithToolCallsVM
import com.intellij.aidebugger.common.viewModels.dataToVM
import com.intellij.aidebugger.python.viewModels.getLastMessage

class LangChainTraceEventModelFactoryProvider : TraceEventViewModelFactoryProvider {
    override fun getViewModelFactories(): List<TraceEventViewModelFactory> {
        return listOf(
            LLMCallFromGenerations(),
            ToolCallsFromAgentAction(),
            PromptTemplate(),
            Finish(),
            Structured(),
        )
    }

    class LLMCallFromGenerations : TraceEventViewModelFactory {
        override fun supportedFrameworks(): Set<Framework> =
            setOf(Framework.LangChain)

        override fun tryCreateView(event: TraceEvent): ViewModelBase? {
            if (event.type != EventType.LlmCall) return null
            val outputs = event.getPayloadOr<Any?>(PayloadKey.Outputs, null) as? Map<*, *> ?: return null
            val generations = getAsGenerationsExpectedTypeOrNull(outputs["generations"]) ?: return null
            val textMessage = getSingleLLMGenerationAndApply(generations) { getTextContentFromMessage(it) } // TODO Claude has both content and tool calls in messages!
            if (textMessage != null) return OutputContentViewVM(textMessage)
            val toolCalls = getSingleLLMGenerationAndApply(generations) { getToolCallsFromMessage(it) }
            if (toolCalls != null && !toolCalls.isEmpty()) return ViewWithToolCallsVM(toolCalls)

            val namedGenerations = namedGenerations(generations)
            return dataToVM(namedGenerations)
        }
    }

    class ToolCallsFromAgentAction : TraceEventViewModelFactory {
        override fun supportedFrameworks(): Set<Framework> =
            setOf(Framework.LangChain)

        override fun tryCreateView(event: TraceEvent): ViewModelBase? {
            val outputs = event.getPayloadOr<Any?>(PayloadKey.Outputs, null) ?: return null
            val lastMessage = getLastMessage(outputs) as? Map<*, *> ?: return null
            if (lastMessage["type"] != "AgentAction") return null
            val toolCall = parseLangChainAgentAction(lastMessage) ?: return null

            return ViewWithToolCallsVM(listOf(toolCall))
        }
    }

    class Finish : TraceEventViewModelFactory {
        override fun supportedFrameworks(): Set<Framework> =
            setOf(Framework.LangChain)

        override fun tryCreateView(event: TraceEvent): ViewModelBase? {
            val outputs = event.getPayloadOr<Any?>(PayloadKey.Outputs, null) ?: return null
            val lastMessage = getLastMessage(outputs) as? Map<*, *> ?: return null
            if (lastMessage["type"] != "AgentFinish") return null
            val returnValues = lastMessage["return_values"] as? Map<*, *> ?: return null
            val output = returnValues["output"] as? String ?: return null

            return InputContentViewVM(output)
        }
    }

    class PromptTemplate : TraceEventViewModelFactory {
        override fun supportedFrameworks(): Set<Framework> =
            setOf(Framework.LangChain)

        override fun tryCreateView(event: TraceEvent): ViewModelBase? {
            if (event.type != EventType.General || event.name != "PromptTemplate") return null
            val inputs = event.getPayloadOr<Any?>(PayloadKey.Inputs, null) as? Map<*, *> ?: return null
            val outputs = event.getPayloadOr<Any?>(PayloadKey.Outputs, null) as? Map<*, *> ?: return null

            val outputMap = outputs["output"] as? Map<*, *> ?: return null
            val promptTemplateType = outputMap["type"] ?: return null
            if (promptTemplateType != "StringPromptValue") return null

            val promptTemplateText = outputMap["text"] as? String ?: return null

            val inputsToDisplay = if (inputs.size == 1) inputs.values.first() else inputs


//            return LangChainPromptTemplateViewModel(inputs, promptTemplateText)
            return InputStructuredContentViewVM(inputsToDisplay!!)
        }
    }


    class Structured : TraceEventViewModelFactory {
        override fun supportedFrameworks(): Set<Framework> = setOf(Framework.LangChainEL)

        override fun tryCreateView(event: TraceEvent): ViewModelBase? {
            if (event.type != EventType.General) return null // TODO there can be several nice views now, but it seems to be broken?
            val outputs = event.getPayloadOr<Any?>(PayloadKey.Outputs, null) as? Map<*, *> ?: return null

//            return LangChainPrettyKeyValueTableViewModel("outputs", outputs)

            val outputsToDisplay = if (outputs.size == 1) outputs.values.first() else outputs

            return OutputStructuredContentViewVM(content = outputsToDisplay!!)
        }
    }

}

fun parseLangChainAgentAction(agentAction: Map<*, *>): ToolCallVM? {
    val name = agentAction["tool"] as? String ?: return null
    val argumentVms = when (val toolInput = agentAction["tool_input"]) {
        is Map<*, *> -> toolInput.map { (key, value) -> ToolArgumentVM(key.toString(), value.toString()) }
        is String -> listOf(ToolArgumentVM("tool_input", toolInput))
        else -> listOf()
    }
    return ToolCallVM(name, argumentVms)
}

fun getAsGenerationsExpectedTypeOrNull(generations: Any?): List<List<Map<String, *>>>? {
    var notEmpty = false
    if (generations !is List<*>) return null

    val outer = ArrayList<List<Map<String, *>>>(generations.size)
    for (inner in generations) {
        if (inner !is List<*>) return null

        val innerList = ArrayList<Map<String, *>>(inner.size)
        for (entry in inner) {
            if (entry !is Map<*, *>) return null
            if (!entry.keys.all { it is String }) return null

            @Suppress("UNCHECKED_CAST")
            innerList += entry as Map<String, *>
            notEmpty = true
        }
        outer += innerList
    }
    if (!notEmpty) return null
    return outer
}

/**
 * outputs["generations"][0][0]["text"]
 */
fun <T> getSingleLLMGenerationAndApply(generations: List<List<Map<String, *>>>, block: (Map<*, *>) -> T?): T? {
    return generations
        .singleOrNull()
        ?.let { it as? List<*> }
        ?.singleOrNull()
        ?.let { it as? Map<*, *> }
        ?.get("message")
        ?.let { it as? Map<*, *> }
        ?.get("kwargs")
        ?.let { it as? Map<*, *> }
        ?.let { block(it) }
}

/**
 * Splits generations per prompt and per generation candidate
 * 1st dimension is different input prompts
 * 2nd dimension is different generation candidates for the same prompt
 */
fun namedGenerations(generations: List<List<*>>): Map<String, Map<String, *>> {
    val result = linkedMapOf<String, LinkedHashMap<String, *>>()

    generations.forEachIndexed { promptIndex, generation ->
        val innerMap = linkedMapOf<String, Any?>()
        result["prompt #$promptIndex"] = innerMap
        generation.forEachIndexed { attemptIndex, attempt ->
            innerMap["candidate #$attemptIndex"] = attempt
        }
    }
    return result
}