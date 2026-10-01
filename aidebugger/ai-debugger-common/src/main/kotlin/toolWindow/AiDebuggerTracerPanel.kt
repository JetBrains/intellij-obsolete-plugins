package com.intellij.aidebugger.common.toolWindow

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.intellij.aidebugger.common.services.DebuggerService
import com.intellij.aidebugger.common.services.GlobalSettingsService
import com.intellij.aidebugger.common.views.AIToolkitThemeData
import com.intellij.aidebugger.common.views.AIToolkitThemeProvider
import com.intellij.aidebugger.common.views.DebuggerDisabledView
import com.intellij.aidebugger.common.views.LicenseRequirementsNotMetView
import com.intellij.aidebugger.common.views.TopViewSelector
import com.intellij.openapi.project.Project
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import javax.swing.JComponent

object AiDebuggerTracerPanel {
    const val TAB_CONTENT_ID = "AiAgentsTracer"
    const val TAB_DISPLAY_NAME = "AI Agents Tracer"

    @OptIn(ExperimentalJewelApi::class)
    fun createTracerPanel(project: Project): JComponent {
        return JewelComposePanel {
            CompositionLocalProvider(
                AIToolkitThemeProvider provides AIToolkitThemeData(),
                LocalIdeaProject provides project,
            ) {
                val isDebuggerEnabled by GlobalSettingsService.getInstance().isDebuggerEnabled.collectAsState()
                val session by DebuggerService.getInstance(project).currentSession.collectAsState()

                if (!isDebuggerEnabled) {
                    DebuggerDisabledView(modifier = Modifier.fillMaxSize())
                    return@CompositionLocalProvider
                }

                if (!isPyCharmPro() && !isIdeaUltimate()) {
                    LicenseRequirementsNotMetView(modifier = Modifier.fillMaxSize())
                    return@CompositionLocalProvider
                }

                TopViewSelector(session?.vm, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
