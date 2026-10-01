package com.intellij.aidebugger.common.extensionPoints

import com.intellij.aidebugger.common.viewModels.TraceEventViewModelFactory
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * An interface for collecting and providing ViewModel factories.
 *
 * Extensions implementing this interface are registered as extension points
 * and provide a list of factories that can create specific ViewModel instances
 * for given events.
 */
interface TraceEventWidgetViewModelFactoryProvider {

    companion object {
        val EP_NAME: ExtensionPointName<TraceEventWidgetViewModelFactoryProvider> = ExtensionPointName
            .create("com.intellij.aidebugger.traceEventWidgetViewModelFactoryProvider")
    }

    /**
     * Retrieves a list of TraceEventViewModelFactory instances capable of creating ViewModelBase instances for specific events.
     *
     * @return A list of TraceEventViewModelFactory instances available for creating ViewModels.
     */
    fun getViewModelFactories(): List<TraceEventViewModelFactory>
}