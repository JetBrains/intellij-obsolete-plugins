package com.intellij.aidebugger.common.views.eventsFeed

import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.extensionPoints.AIDebuggerToolWindowExtension

/**
 * Retrieves the default text to display in an empty AI Debugger Tool Window.
 *
 * This method checks the registered extensions for the AI Debugger Tool Window and retrieves the
 * message defined by the first available extension. If no extensions are available, it falls back
 * to a default message defined in the `AiDebuggerBundle`.
 *
 * @return The empty feed message to display in the AI Debugger Tool Window.
 */
fun getAiDebuggerToolWindowEmptyText(): String {
    val extensions = AIDebuggerToolWindowExtension.EP_NAME.extensionList
    val emptyFeedMessage = extensions.firstOrNull()?.getEmptyFeedMessage()
        ?: AiDebuggerBundle.message("aitoolkit.feed.empty.text")

    return emptyFeedMessage
}