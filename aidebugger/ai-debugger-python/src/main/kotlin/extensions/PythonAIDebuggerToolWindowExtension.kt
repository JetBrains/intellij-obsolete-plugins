package com.intellij.aidebugger.python.extensions

import com.intellij.aidebugger.common.extensionPoints.AIDebuggerToolWindowExtension
import com.intellij.aidebugger.python.AiDebuggerPythonBundle

/**
 * A specific implementation of the AIDebuggerToolWindowExtension, tailored for Python AI debugging integrations.
 *
 * This class provides custom behavior when the feed in the AI Debugger tool window is empty,
 * specifically for Python-related projects.
 */
class PythonAIDebuggerToolWindowExtension : AIDebuggerToolWindowExtension {

    override fun getEmptyFeedMessage(): String {
        return AiDebuggerPythonBundle.message("aitoolkit.feed.empty.text")
    }
}