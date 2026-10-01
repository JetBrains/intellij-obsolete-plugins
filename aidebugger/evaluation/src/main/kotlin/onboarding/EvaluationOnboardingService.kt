package com.intellij.aidebugger.evaluation.onboarding

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private const val ONBOARDING_TOUR_PROPERTY_VALUE = "evaluation.onboarding.tour"

private var onboardingPassed: Boolean
    get() = PropertiesComponent.getInstance().getBoolean(ONBOARDING_TOUR_PROPERTY_VALUE)
    set(value) {
        PropertiesComponent.getInstance().setValue(ONBOARDING_TOUR_PROPERTY_VALUE, value)
    }

@Service(Service.Level.PROJECT)
class EvaluationOnboardingService(private val project: Project, private val cs: CoroutineScope) : Disposable {
    private val myTourInProgress = AtomicBoolean(false)

    fun onboardIfNecessary() {
        if (onboardingPassed) {
            return
        }
        startOnboarding()
        onboardingPassed = true
    }

    fun resetOnboardingPassed() {
        onboardingPassed = false
    }

    fun startOnboarding(repeat: Boolean = false) {
        val alreadyInProgress = myTourInProgress.getAndSet(true)
        if (alreadyInProgress) return

        val steps = getSteps(repeat)
        val executor = EvaluationOnboardingExecutor(project, steps, cs, this, repeat)
        cs.launch(Dispatchers.EDT) { executor.start() }
    }

    private fun getSteps(repeat: Boolean = false): List<Pair<String, EvaluationOnboardingStep>> {
        val stepIds = if (repeat) getRepeatStepsOrder() else getDefaultStepsOrder()
        return stepIds.mapNotNull { id ->
            val step = EvaluationOnboardingStep.getIfAvailable(id)
            if (step != null) id to step else null
        }
    }

    private fun getDefaultStepsOrder(): List<String> {
        return listOf(
            com.intellij.aidebugger.evaluation.onboarding.steps.IntroStep.STEP_KEY,
            com.intellij.aidebugger.evaluation.onboarding.steps.AddToDatasetButtonStep.STEP_KEY,
            com.intellij.aidebugger.evaluation.onboarding.steps.EnterDatasetNameStep.STEP_KEY,
            com.intellij.aidebugger.evaluation.onboarding.steps.SelectDatasetStep.STEP_KEY,
            com.intellij.aidebugger.evaluation.onboarding.steps.AddToDatasetDialogInputsStep.STEP_KEY,
            com.intellij.aidebugger.evaluation.onboarding.steps.SwitchToDatasetsFolderStep.STEP_KEY,
            com.intellij.aidebugger.evaluation.onboarding.steps.SwitchToEvaluationFolderStep.STEP_KEY,
            com.intellij.aidebugger.evaluation.onboarding.steps.EvaluationPlusButtonStep.STEP_KEY,
            com.intellij.aidebugger.evaluation.onboarding.steps.CreateConfigurationFormStep.STEP_KEY,
            com.intellij.aidebugger.evaluation.onboarding.steps.EvaluationRunButtonStep.STEP_KEY,
            com.intellij.aidebugger.evaluation.onboarding.steps.EvaluationResultsOutputStep.STEP_KEY,
            com.intellij.aidebugger.evaluation.onboarding.steps.EvaluationRunCloudButtonStep.STEP_KEY,
            com.intellij.aidebugger.evaluation.onboarding.steps.HelpButtonStep.STEP_KEY,
        )
    }

    private fun getRepeatStepsOrder(): List<String> {
        return getDefaultStepsOrder().subList(1, getDefaultStepsOrder().size)
    }

    override fun dispose() {}

    fun onboardingFinished() {
        myTourInProgress.set(false)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): EvaluationOnboardingService = project.service()
    }
}