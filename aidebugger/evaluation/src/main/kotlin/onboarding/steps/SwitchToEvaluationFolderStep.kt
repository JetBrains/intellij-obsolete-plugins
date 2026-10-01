package com.intellij.aidebugger.evaluation.onboarding.steps

import com.intellij.aidebugger.common.onboarding.OnboardingAnchorKeys
import com.intellij.aidebugger.evaluation.EvaluationBundle
import com.intellij.aidebugger.evaluation.onboarding.EvaluationOnboardingStep
import com.intellij.aidebugger.evaluation.onboarding.EvaluationOnboardingStepData
import com.intellij.aidebugger.evaluation.onboarding.OnboardingAnchorRegistry
import com.intellij.aidebugger.evaluation.onboarding.Placement
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.CheckedDisposable

class SwitchToEvaluationFolderStep : EvaluationOnboardingStep {
    override fun isAvailable(): Boolean = true

    override suspend fun performStep(project: Project, disposable: CheckedDisposable): EvaluationOnboardingStepData {
        return EvaluationOnboardingStepData(
            title = EvaluationBundle.message("onboarding.switch.to.evaluation.step.header"),
            message = EvaluationBundle.message("onboarding.switch.to.evaluation.step.text"),
            anchorBoundsProvider = { OnboardingAnchorRegistry.get(OnboardingAnchorKeys.EVALUATION_TOOLWINDOW_EVALUATION_FOLDER) },
            placement = Placement.Above,
            onDoneActionName = EvaluationBundle.message("onboarding.gotIt.button.next"),
        )
    }

    override val stepId: String = STEP_KEY

    companion object {
        const val STEP_KEY = "switch.to.evaluation.folder"
    }
}
