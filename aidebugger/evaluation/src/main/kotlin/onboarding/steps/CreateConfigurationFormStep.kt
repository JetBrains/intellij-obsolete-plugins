package com.intellij.aidebugger.evaluation.onboarding.steps

import com.intellij.aidebugger.common.onboarding.OnboardingAnchorKeys
import com.intellij.aidebugger.evaluation.EvaluationBundle
import com.intellij.aidebugger.evaluation.onboarding.EvaluationOnboardingStep
import com.intellij.aidebugger.evaluation.onboarding.EvaluationOnboardingStepData
import com.intellij.aidebugger.evaluation.onboarding.OnboardingAnchorRegistry
import com.intellij.aidebugger.evaluation.onboarding.Placement
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CreateConfigurationFormStep : EvaluationOnboardingStep {
    override fun isAvailable(): Boolean = true

    override suspend fun performStep(project: Project, disposable: CheckedDisposable): EvaluationOnboardingStepData {
        return EvaluationOnboardingStepData(
            message = EvaluationBundle.message("onboarding.create.config.form.step.text"),
            anchorBoundsProvider = { OnboardingAnchorRegistry.get(OnboardingAnchorKeys.CREATE_CONFIG_DIALOG_RUN_CONFIGURATION) },
            placement = Placement.Start,
            highlightAnchor = false,
            onDoneActionName = EvaluationBundle.message("onboarding.gotIt.button.next"),
            registerAutoAdvance = { scope, stepDisposable, advance ->
                val job = scope.launch {
                    if (OnboardingAnchorRegistry.getFlag(OnboardingAnchorKeys.CREATE_CONFIG_CONFIRMED) == true) {
                        advance(); return@launch
                    }

                    OnboardingAnchorRegistry
                        .observeFlag(OnboardingAnchorKeys.CREATE_CONFIG_CONFIRMED)
                        .filter { it }
                        .first()
                    advance()
                }
                Disposer.register(stepDisposable) { job.cancel() }
            }
        )
    }

    override val stepId: String = STEP_KEY

    companion object {
        const val STEP_KEY = "create.config.form"
    }
}
