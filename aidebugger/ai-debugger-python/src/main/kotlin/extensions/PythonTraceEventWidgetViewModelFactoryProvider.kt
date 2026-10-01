package com.intellij.aidebugger.python.extensions

import com.intellij.aidebugger.common.extensionPoints.TraceEventWidgetViewModelFactoryProvider
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.models.entities.PayloadKey
import com.intellij.aidebugger.common.models.entities.TraceEvent
import com.intellij.aidebugger.common.models.entities.getPayloadOr
import com.intellij.aidebugger.common.utility.formatMilliseconds
import com.intellij.aidebugger.common.viewModels.CommonWidgetVM
import com.intellij.aidebugger.common.viewModels.TraceEventViewModelFactory
import com.intellij.aidebugger.common.viewModels.ViewModelBase
import com.intellij.aidebugger.python.viewModels.getLastMessage

class PythonTraceEventWidgetViewModelFactoryProvider: TraceEventWidgetViewModelFactoryProvider {
    override fun getViewModelFactories(): List<TraceEventViewModelFactory> = listOf(
        TraceEventTokensSpentVMFactory(),
        TraceEventEnterExitDurationFactory(),
    )
}

class TraceEventEnterExitDurationFactory: TraceEventViewModelFactory {
    override fun supportedFrameworks(): Set<Framework> = setOf(Framework.LangChain, Framework.LangChainEL, Framework.LangGraph)

    override fun tryCreateView(event: TraceEvent): ViewModelBase? {
        val duration = event.timestampEndMs - event.timestampStartMs

        if (duration <= 0) return null

        return CommonWidgetVM(
            iconKey = "widgets/duration",
            text = formatMilliseconds(duration)
        )
    }
}

class TraceEventTokensSpentVMFactory: TraceEventViewModelFactory {
    override fun supportedFrameworks(): Set<Framework> = setOf(Framework.LangChain, Framework.LangChainEL, Framework.LangGraph)

    override fun tryCreateView(event: TraceEvent): ViewModelBase? {
        val outputs = event.getPayloadOr<Any?>(PayloadKey.Outputs, null) ?: return null
        val message = getLastMessage(outputs) as? Map<*, *> ?: return null
        val responseMetadata = message["response_metadata"] as? Map<*, *> ?: return null
        val tokenUsage = responseMetadata["token_usage"] as? Map<*, *> ?: return null
        val totalTokens = tokenUsage["total_tokens"] as? Double ?: return null


        return CommonWidgetVM(
            iconKey = "widgets/tokens",
            text = totalTokens.toInt().toString()
        )
    }
}