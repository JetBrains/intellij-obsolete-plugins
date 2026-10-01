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
interface TraceEventViewModelFactoryProvider {

    companion object {
        val EP_NAME: ExtensionPointName<TraceEventViewModelFactoryProvider> = ExtensionPointName
            .create("com.intellij.aidebugger.traceEventViewModelFactoryProvider")
    }

    /**
     * Retrieves a list of ViewFactory instances capable of creating ViewModelBase instances for specific events.
     *
     * @return A list of ViewFactory instances available for creating ViewModels.
     */
    fun getViewModelFactories(): List<TraceEventViewModelFactory>
}
