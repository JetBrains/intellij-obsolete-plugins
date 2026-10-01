package com.intellij.aidebugger.koog.extension

import com.intellij.aidebugger.common.extensionPoints.AIDebuggerToolWindowExtension
import com.intellij.aidebugger.koog.AiDebuggerKoogBundle

/**
 * An implementation of the `AIDebuggerToolWindowExtension` interface that provides
 * a custom empty feed message for the AI Debugger tool window.
 *
 * This class is designed to extend the functionality of the AI Debugger tool window
 * by customizing messages displayed in specific scenarios, such as when the feed is empty.
 */
class KoogAIDebuggerToolWindowExtension : AIDebuggerToolWindowExtension {

    override fun getEmptyFeedMessage(): String {
        return AiDebuggerKoogBundle.message("aitoolkit.feed.empty.text")
    }
}
