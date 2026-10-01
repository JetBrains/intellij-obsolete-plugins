package com.intellij.aidebugger.common.toolWindow

import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.common.ShowReason
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.plugins.PluginManagerCore.ULTIMATE_PLUGIN_ID
import com.intellij.ide.plugins.PluginManagerCore.loadedPlugins
import com.intellij.openapi.application.EDT
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.PlatformUtils
import com.intellij.xdebugger.XDebuggerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus

suspend fun openAiDebuggerToolWindow(project: Project) = withContext(Dispatchers.EDT) {
    val toolWindowManager = ToolWindowManager.getInstance(project)
    val debugToolWindow = toolWindowManager.getToolWindow(ToolWindowId.DEBUG) ?: return@withContext

    if (!debugToolWindow.isVisible) {
        debugToolWindow.show()
    }

    // Select the AI Agents Tracer tab in the current debug session's RunnerLayoutUi
    val currentSession = XDebuggerManager.getInstance(project).currentSession
    val ui = currentSession?.ui
    if (ui != null) {
        val tracerContent = ui.findContent(AiDebuggerTracerPanel.TAB_CONTENT_ID)
        if (tracerContent != null) {
            ui.selectAndFocus(tracerContent, false, false)
        }
    }

    AiDebuggerCollector.reportToolWindowShown(project, -1, -1, ShowReason.AUTO)
}

fun isAiDebuggerToolWindowVisible(project: Project): Boolean {
    val debugToolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DEBUG)
        ?: return false
    if (!debugToolWindow.isVisible) return false

    val currentSession = XDebuggerManager.getInstance(project).currentSession ?: return false
    return currentSession.ui?.findContent(AiDebuggerTracerPanel.TAB_CONTENT_ID) != null
}

fun isLoaded(id: PluginId): Boolean {
    val plugin = loadedPlugins.find { it.pluginId == id }
    return plugin != null && isLoaded(plugin)
}

@Suppress("UnstableApiUsage")
@ApiStatus.Experimental
fun isLoaded(plugin: PluginDescriptor): Boolean = (plugin as? IdeaPluginDescriptorImpl)?.pluginClassLoader != null


@Suppress("UnstableApiUsage")
fun isPyCharmPro(): Boolean {
    return PlatformUtils.isPyCharm()
            && isLoaded(ULTIMATE_PLUGIN_ID)
            && !PluginManagerCore.isDisabled(ULTIMATE_PLUGIN_ID)
}

@Suppress("UnstableApiUsage")
fun isIdeaUltimate(): Boolean {
    return PlatformUtils.isIdeaUltimate()
}
