package com.intellij.aidebugger.evaluation

import com.intellij.aidebugger.common.onboarding.OnboardingAnchorKeys
import com.intellij.aidebugger.common.onboarding.OnboardingRuntimeFlags
import com.intellij.aidebugger.common.services.GlobalSettingsService
import com.intellij.aidebugger.evaluation.onboarding.EvaluationOnboardingService
import com.intellij.aidebugger.evaluation.onboarding.OnboardingAnchorRegistry
import com.intellij.aidebugger.evaluation.onboarding.ui.componentToRect
import com.intellij.aidebugger.evaluation.onboarding.ui.findHelpButton
import com.intellij.aidebugger.evaluation.onboarding.ui.getDatasetsTabRect
import com.intellij.aidebugger.evaluation.onboarding.ui.getEvaluationTabRect
import com.intellij.aidebugger.evaluation.views.DatasetsView
import com.intellij.aidebugger.evaluation.views.EvaluationView
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import javax.swing.SwingUtilities

private val HELP_TOOLTIP_TEXT get() = EvaluationBundle.message("eval.view.help.button.tooltip")
private val HELP_TOOLTIP_TEXT_DISABLED get() = EvaluationBundle.message("eval.view.help.button.tooltip.disabled")


fun isRunnerToolWindowVisible(project: Project): Boolean =
    ToolWindowManager.getInstance(project).getToolWindow(EvaluationToolWindowFactory.ID)?.isVisible ?: false

class EvaluationToolWindowFactory : ToolWindowFactory, DumbAware {
    companion object {
        const val ID: String = "AI Toolkit: Evaluation"
        private const val PLACED_FLAG = "com.intellij.aidebugger.runnerToolWindow.placedBottom.v2"
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        return GlobalSettingsService.getInstance().isEvaluationEnabled.value
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.component.putClientProperty(ToolWindowContentUi.DONT_HIDE_TOOLBAR_IN_HEADER, true)
        SwingUtilities.invokeLater { registerOnboardingAnchorsForToolWindow(toolWindow) }

        val connection = project.messageBus.connect()
        connection.subscribe(
            topic = ToolWindowManagerListener.TOPIC,
            handler = EvaluationToolWindowState(project)
        )

        val datasetsView = DatasetsView(project)
        val datasetsContent = ContentFactory.getInstance().createContent(datasetsView, EvaluationBundle.message("eval.toolwindow.tab.datasets"), false)
        datasetsContent.isCloseable = false
        datasetsContent.setDisposer { datasetsView.dispose() }
        toolWindow.contentManager.addContent(datasetsContent)

        val evaluationView = EvaluationView(project)
        val evaluationContent = ContentFactory.getInstance().createContent(evaluationView, EvaluationBundle.message("eval.toolwindow.tab.evaluation"), false)
        evaluationContent.isCloseable = false
        evaluationContent.setDisposer { evaluationView.dispose() }
        toolWindow.contentManager.addContent(evaluationContent)

        toolWindow.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
                    when (event.content) {
                        datasetsContent -> EvaluationCollector.reportDatasetsTabSelected(project)
                        evaluationContent -> EvaluationCollector.reportEvaluationTabSelected(project)
                    }
                }
            }
        })

        selectInitialTab(toolWindow)
        placeBottomOnce(project, toolWindow)

        createHelpButton(project, toolWindow)
    }

    private fun registerOnboardingAnchorsForToolWindow(toolWindow: ToolWindow) {
        try {
            OnboardingAnchorRegistry.setProvider(OnboardingAnchorKeys.EVALUATION_TOOLWINDOW_EVALUATION_FOLDER) {
                selectEvaluationTab(toolWindow)
                getEvaluationTabRect(toolWindow.component)
            }
            OnboardingAnchorRegistry.setProvider(OnboardingAnchorKeys.EVALUATION_TOOLWINDOW_DATASETS_FOLDER) {
                selectDatasetsTab(toolWindow)
                getDatasetsTabRect(toolWindow.component)
            }
            OnboardingAnchorRegistry.setProvider(OnboardingAnchorKeys.EVALUATION_VIEW_HELP_BUTTON) {
                val c = findHelpButton(toolWindow.component)
                if (c != null) componentToRect(c) else null
            }
        } catch (_: Throwable) {
        }
    }

    private fun createHelpButton(project: Project, toolWindow: ToolWindow) {
        try {
            val helpAction = object : AnAction(HELP_TOOLTIP_TEXT, HELP_TOOLTIP_TEXT, AllIcons.Actions.Help) {
                override fun actionPerformed(e: AnActionEvent) {
                    EvaluationCollector.reportOnboardingRestarted()
                    EvaluationOnboardingService.getInstance(project).startOnboarding(true)
                }

                override fun getActionUpdateThread() = ActionUpdateThread.EDT

                override fun update(e: AnActionEvent) {
                    val active = runCatching { OnboardingRuntimeFlags.onboardingActive }.getOrDefault(false)
                    val presentation = e.presentation
                    if (active) {
                        presentation.isEnabled = false
                        presentation.text = HELP_TOOLTIP_TEXT_DISABLED
                    } else {
                        presentation.isEnabled = true
                        presentation.text = HELP_TOOLTIP_TEXT
                    }
                }
            }
            toolWindow.setTitleActions(listOf(helpAction))
        } catch (_: Throwable) {
        }
    }

    private fun selectInitialTab(toolWindow: ToolWindow) {
        try {
            val contents = toolWindow.contentManager.contents
            if (contents.size >= 2) {
                toolWindow.contentManager.setSelectedContent(contents[1], true)
            }
        } catch (_: Throwable) {
        }
    }

    private fun selectEvaluationTab(toolWindow: ToolWindow) {
        try {
            val contents = toolWindow.contentManager.contents
            if (contents.size >= 2) {
                toolWindow.contentManager.setSelectedContent(contents[1], true)
            }
        } catch (_: Throwable) {
        }
    }

    private fun selectDatasetsTab(toolWindow: ToolWindow) {
        try {
            val contents = toolWindow.contentManager.contents
            if (contents.size >= 2) {
                toolWindow.contentManager.setSelectedContent(contents[0], true)
            }
        } catch (_: Throwable) {
        }
    }

    private fun placeBottomOnce(project: Project, toolWindow: ToolWindow) {
        val props = PropertiesComponent.getInstance(project)
        if (props.getBoolean(PLACED_FLAG, false)) return

        try {
            toolWindow.setAnchor(ToolWindowAnchor.BOTTOM, null)
            toolWindow.setSplitMode(true, null)
        } catch (_: Throwable) {
        } finally {
            props.setValue(PLACED_FLAG, true)
        }
    }
}

@Suppress("UnstableApiUsage")
class EvaluationToolWindowState(
    private val project: Project
): ToolWindowManagerListener {
    private var wasVisible: Boolean = false

    override fun stateChanged(toolWindowManager: ToolWindowManager) {
        val isVisible = isRunnerToolWindowVisible(project)

        if (wasVisible == isVisible) return
        wasVisible = isVisible

        @Suppress("ControlFlowWithEmptyBody")
        if (isVisible) {
        }
        else {
        }
    }
}