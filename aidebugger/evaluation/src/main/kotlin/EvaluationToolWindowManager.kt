
package com.intellij.aidebugger.evaluation

import com.intellij.aidebugger.common.services.GlobalSettingsService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class EvaluationToolWindowManager(private val project: Project, private val scope: CoroutineScope) {

    init {
        scope.launch {
            GlobalSettingsService.getInstance().isEvaluationEnabled.collect { enabled ->
                // invokeLater ensures we're on EDT when modifying UI
                ApplicationManager.getApplication().invokeLater {
                    updateToolWindowVisibility(enabled)
                }
            }
        }

        // Initial setup - delay to ensure project is fully initialized
        ApplicationManager.getApplication().invokeLater {
            val initialEnabled = GlobalSettingsService.getInstance().isEvaluationEnabled.value
            updateToolWindowVisibility(initialEnabled)
        }
    }

    private fun registerToolWindow() {
        // This should now be called from EDT, so no need to wrap again
        val toolWindowManager = ToolWindowManager.getInstance(project)

        // Check if already registered (in case of multiple initializations)
        if (toolWindowManager.getToolWindow(EvaluationToolWindowFactory.ID) != null) {
            return
        }

        val toolWindowIcon = IconLoader.getIcon(
            "/icons/evalToolWindowIcon.svg",
            EvaluationToolWindowFactory::class.java
        )

        toolWindowManager.registerToolWindow(EvaluationToolWindowFactory.ID) {
            anchor = ToolWindowAnchor.BOTTOM
            icon = toolWindowIcon
            contentFactory = EvaluationToolWindowFactory()
            canCloseContent = true
            stripeTitle = java.util.function.Supplier { EvaluationToolWindowFactory.ID }
            hideOnEmptyContent = true
        }
    }

    private fun updateToolWindowVisibility(enabled: Boolean) {
        // Caller ensures this runs on EDT
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val existingToolWindow = toolWindowManager.getToolWindow(EvaluationToolWindowFactory.ID)

        if (enabled) {
            if (existingToolWindow == null) {
                registerToolWindow()
            } else {
                existingToolWindow.isAvailable = true
                existingToolWindow.isShowStripeButton = true
            }
        } else {
            if (existingToolWindow != null) {
                existingToolWindow.isShowStripeButton = false
                existingToolWindow.isAvailable = false
                if (existingToolWindow.isVisible) {
                    existingToolWindow.hide(null)
                }
                existingToolWindow.remove()
            }
        }
        // Force immediate save after toolWindow changes
        project.save()
    }
}
