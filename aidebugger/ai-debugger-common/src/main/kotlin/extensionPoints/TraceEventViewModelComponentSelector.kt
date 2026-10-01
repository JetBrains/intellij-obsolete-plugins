package com.intellij.aidebugger.common.extensionPoints

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.intellij.aidebugger.common.viewModels.EventVM
import com.intellij.openapi.extensions.ExtensionPointName
import kotlin.reflect.KClass

/**
 * Implementations render a specific EventVM subtype.
 */
interface TraceEventViewModelComponentSelector<T : EventVM> {

    companion object {
        val EP_NAME: ExtensionPointName<TraceEventViewModelComponentSelector<*>> =
            ExtensionPointName.create("com.intellij.aidebugger.traceEventViewModelComponentSelector")
    }

    /**
     * The concrete VM class this selector can render.
     * Keeping this avoids type erasure problems when picking an implementation.
     */
    val vmClass: KClass<T>

    /**
     * Render the component for the exact VM subtype.
     */
    @Composable
    fun componentSelector(viewModel: T, modifier: Modifier = Modifier): (@Composable () -> Unit)?
}