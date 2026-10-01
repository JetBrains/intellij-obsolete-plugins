package com.intellij.aidebugger.koog.extension

import ai.koog.prompt.message.Message
import com.intellij.aidebugger.common.extensionPoints.TraceEventWidgetViewModelFactoryProvider
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.models.entities.PayloadKey
import com.intellij.aidebugger.common.models.entities.TraceEvent
import com.intellij.aidebugger.common.utility.formatMilliseconds
import com.intellij.aidebugger.common.viewModels.CommonWidgetVM
import com.intellij.aidebugger.common.viewModels.TraceEventViewModelFactory
import com.intellij.aidebugger.common.viewModels.ViewModelBase
import com.intellij.openapi.diagnostic.thisLogger

class KoogTraceEventWidgetViewModelFactoryProvider : TraceEventWidgetViewModelFactoryProvider {
    override fun getViewModelFactories(): List<TraceEventViewModelFactory> = listOf(
            KoogTokensSpentVMFactory(),
            KoogEnterExitDurationFactory(),
        )

    /**
     * A factory class responsible for creating view models that represent the duration
     * of a `TraceEvent` specific to the `Koog` framework.
     *
     * This factory ensures that the generated view model is only created if the event
     * represents a valid duration based on its start and end timestamps.
     */
    private class KoogEnterExitDurationFactory: TraceEventViewModelFactory {
        override fun supportedFrameworks(): Set<Framework> = setOf(Framework.Koog)

        override fun tryCreateView(event: TraceEvent): ViewModelBase? {
            val duration = event.timestampEndMs - event.timestampStartMs

            if (duration <= 0) return null

            return CommonWidgetVM(
                iconKey = "widgets/duration",
                text = formatMilliseconds(duration)
            )
        }
    }

    /**
     * Factory class responsible for creating a ViewModel that displays the total number
     * of tokens spent during an LLM call in the Koog framework.
     *
     * This factory processes `TraceEvent`s of type [EventType.LlmCall] and extracts the
     * token counts from the response metadata. The resulting `CommonWidgetVM` displays
     * the total token usage alongside a token icon.
     */
    private class KoogTokensSpentVMFactory: TraceEventViewModelFactory {

        companion object {
            private val logger = thisLogger()
        }

        override fun supportedFrameworks(): Set<Framework> = setOf(Framework.Koog)

        override fun tryCreateView(event: TraceEvent): ViewModelBase? =
            when (event.type) {
                EventType.LlmCall -> {
                    val outputs = event.payload[PayloadKey.Outputs] as? List<*> ?: return null

                    // Calculate the total spent for each response
                    val totalTokens = outputs.sumOf { output ->
                        val response = output as? Message.Response ?: return@sumOf 0
                        response.metaInfo.inputTokensCount ?: return@sumOf 0
                    }

                    CommonWidgetVM(
                        iconKey = "widgets/tokens",
                        text = totalTokens.toString()
                    )
                }

                else -> null
            }
    }
}