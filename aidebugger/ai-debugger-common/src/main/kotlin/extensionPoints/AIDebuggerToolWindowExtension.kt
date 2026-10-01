package com.intellij.aidebugger.common.extensionPoints

import com.intellij.openapi.extensions.ExtensionPointName

/**
 * Interface for extending the AI Debugger tool window functionality.
 *
 * Implementations of this interface can provide additional customizations
 * or behaviors to the AI Debugger tool window by being registered as an extension point.
 */
interface AIDebuggerToolWindowExtension {

    companion object {
        val EP_NAME: ExtensionPointName<AIDebuggerToolWindowExtension> = ExtensionPointName
            .create("com.intellij.aidebugger.toolWindowExtension")
    }

    fun getEmptyFeedMessage(): String
}
