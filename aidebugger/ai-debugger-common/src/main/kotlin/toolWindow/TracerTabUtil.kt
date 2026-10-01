package com.intellij.aidebugger.common.toolWindow

import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.execution.ui.layout.impl.RunnerLayoutUiImpl
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.GotItTooltip
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.tabs.impl.TabLabel
import javax.swing.JComponent

object TracerTabUtil {
    private const val GOT_IT_ID = "ai.agents.tracer.new.tab"

    fun addTracerTab(project: Project, ui: RunnerLayoutUi) {
        if (ui.findContent(AiDebuggerTracerPanel.TAB_CONTENT_ID) != null) return

        val panel = AiDebuggerTracerPanel.createTracerPanel(project)
        val content = ui.createContent(
            AiDebuggerTracerPanel.TAB_CONTENT_ID,
            panel,
            AiDebuggerTracerPanel.TAB_DISPLAY_NAME,
            AllIcons.General.New_badge,
            null
        )
        content.isCloseable = false
        ui.addContent(content, 2, PlaceInGrid.bottom, false)

        content.fireAlert()

        if (ui is RunnerLayoutUiImpl) {
            val tabs = ui.contentUI.tabs
            val tabInfo = tabs.tabs.firstOrNull { it.text == content.displayName }
            val label = tabInfo?.let { tabs.getTabLabel(it) as? JComponent }
            if (label != null) {
                (label as? TabLabel)?.let {
                    (it.labelComponent as? SimpleColoredComponent)?.isIconOnTheRight = true
                }
                GotItTooltip(GOT_IT_ID, AiDebuggerBundle.message("aitoolkit.tracer.gotit.body"))
                    .show(label, GotItTooltip.BOTTOM_MIDDLE)
            }
        }
    }
}
