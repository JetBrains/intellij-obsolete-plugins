package com.intellij.aidebugger.evaluation.onboarding

import com.intellij.aidebugger.common.onboarding.OnboardingAnchorKeys
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class StartOnboardingListener(
    private val project: Project,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            if (OnboardingAnchorRegistry.getFlag(
                    OnboardingAnchorKeys.THREADS_NAVIGATION_OPEN_START_ONBOARDING
                ) == true
            ) {
                startOnboarding()
                return@launch
            }

            OnboardingAnchorRegistry
                .observeFlag(OnboardingAnchorKeys.THREADS_NAVIGATION_OPEN_START_ONBOARDING)
                .filter { it }
                .first()

            startOnboarding()
        }
    }

    private fun startOnboarding() {
        EvaluationOnboardingService.getInstance(project).onboardIfNecessary()
        OnboardingAnchorRegistry.removeFlag(
            OnboardingAnchorKeys.THREADS_NAVIGATION_OPEN_START_ONBOARDING
        )
    }
}