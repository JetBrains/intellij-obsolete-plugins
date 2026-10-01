package com.intellij.aidebugger.evaluation.onboarding

import com.intellij.openapi.extensions.ExtensionPointName

class EvaluationOnboardingBean {
    companion object {
        private val EP_NAME: ExtensionPointName<EvaluationOnboardingBean> = ExtensionPointName("com.intellij.aidebugger.evaluation.onboarding")

        fun getInstance(): EvaluationOnboardingBean {
            return EP_NAME.findFirstSafe { true } ?: error("NewUiOnboarding bean must be defined")
        }
    }
}