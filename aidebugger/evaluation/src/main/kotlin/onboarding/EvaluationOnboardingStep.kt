package com.intellij.aidebugger.evaluation.onboarding

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.util.KeyedLazyInstanceEP

interface EvaluationOnboardingStep {
    suspend fun performStep(project: Project, disposable: CheckedDisposable): EvaluationOnboardingStepData?

    fun isAvailable(): Boolean = true

    val stepId: String

    companion object {
        val EP_NAME: ExtensionPointName<KeyedLazyInstanceEP<EvaluationOnboardingStep>> =
            ExtensionPointName.Companion.create("com.intellij.aidebugger.evaluation.onboarding.step")

        fun getIfAvailable(stepId: String): EvaluationOnboardingStep? {
            val step = EP_NAME.findFirstSafe { it.key == stepId }?.instance
            return if (step?.isAvailable() == true) {
                step
            } else null
        }
    }
}