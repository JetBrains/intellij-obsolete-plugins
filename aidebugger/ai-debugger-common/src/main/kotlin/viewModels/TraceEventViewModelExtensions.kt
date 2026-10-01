package com.intellij.aidebugger.common.viewModels

import com.intellij.aidebugger.common.extensionPoints.TraceEventViewModelFactoryProvider
import com.intellij.aidebugger.common.extensionPoints.TraceEventWidgetViewModelFactoryProvider
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.models.entities.TraceEvent

fun getNicerViews(event: TraceEvent): List<ViewModelBase> {
    val factories: List<TraceEventViewModelFactory> = buildList {
        val extensions = TraceEventViewModelFactoryProvider.EP_NAME.extensionList
        addAll(extensions.flatMap { extension -> extension.getViewModelFactories() })
    }

    return factories
        .filter { it.supports(event.framework) }
        .mapNotNull { it.tryCreateView(event) }
}

fun getNodeWidgets(event: TraceEvent): List<ViewModelBase> {
    val factories: List<TraceEventViewModelFactory> = buildList {
        val extensions = TraceEventWidgetViewModelFactoryProvider.EP_NAME.extensionList
        addAll(extensions.flatMap { extension -> extension.getViewModelFactories() })
    }

    return factories
        .filter { it.supports(event.framework) }
        .mapNotNull { it.tryCreateView(event) }
}

interface TraceEventViewModelFactory {
    // we only have Companions in supportedFrameworks(), so we need to access its parent class, not the Companion class
    fun supports(framework: Framework): Boolean = framework in supportedFrameworks()

    fun supportedFrameworks(): Set<Framework>
    fun tryCreateView(event: TraceEvent): ViewModelBase?
}