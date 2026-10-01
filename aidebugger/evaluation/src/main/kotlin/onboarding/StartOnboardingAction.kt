package com.intellij.aidebugger.evaluation.onboarding

import com.intellij.aidebugger.evaluation.EvaluationBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.IconManager

class StartOnboardingAction : DumbAwareAction(
    EvaluationBundle.message("actions.start.onboarding.name"),
    EvaluationBundle.message("actions.start.onboarding.description"),
    IconManager.getInstance().getIcon("icons/toolWindowIcon.svg", StartOnboardingAction::class.java.classLoader)
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        startOnboarding(project)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        // Enable the action only if a project is available
        e.presentation.isEnabled = e.project != null
    }

    private fun startOnboarding(project: Project) {
        EvaluationOnboardingService.getInstance(project).startOnboarding()
    }
}