package com.intellij.aidebugger.common

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import org.jetbrains.annotations.NonNls

object AiDebuggerPlugin {
    const val PLUGIN_ID: @NonNls String = "com.intellij.aidebugger"
    const val DEV_VERSION: @NonNls String = "dev"

    // Plugin version string field to be attached to every event
    val PLUGIN_VERSION: String = PluginManagerCore.getPlugin(
        PluginId.getId(PLUGIN_ID)
    )?.version ?: DEV_VERSION

    val scriptsPath: String
        get() = "${pluginDescriptor.pluginPath}/python"

    val aiDebuggerPath: String
        get() = "${scriptsPath}/ai_profiler.py"

    private val pluginDescriptor: IdeaPluginDescriptor
        get() = PluginManagerCore.getPlugin(pluginId) ?: error("The $PLUGIN_ID plugin cannot find itself")

    private val pluginId: PluginId
        get() = PluginId.findId(PLUGIN_ID) ?: error("No ID found")
}