package com.intellij.aidebugger.evaluation.onboarding

import com.intellij.aidebugger.common.onboarding.OnboardingAnchorKeys
import com.intellij.aidebugger.common.onboarding.OnboardingRuntimeFlags
import com.intellij.aidebugger.evaluation.EvaluationBundle
import com.intellij.aidebugger.evaluation.onboarding.steps.AddToDatasetButtonStep
import com.intellij.aidebugger.evaluation.onboarding.steps.AddToDatasetDialogInputsStep
import com.intellij.aidebugger.evaluation.onboarding.steps.CreateConfigurationFormStep
import com.intellij.aidebugger.evaluation.onboarding.steps.EnterDatasetNameStep
import com.intellij.aidebugger.evaluation.onboarding.steps.EvaluationPlusButtonStep
import com.intellij.aidebugger.evaluation.onboarding.steps.EvaluationResultsOutputStep
import com.intellij.aidebugger.evaluation.onboarding.steps.EvaluationRunButtonStep
import com.intellij.aidebugger.evaluation.onboarding.steps.EvaluationRunCloudButtonStep
import com.intellij.aidebugger.evaluation.onboarding.steps.IntroStep
import com.intellij.aidebugger.evaluation.onboarding.steps.SelectDatasetStep
import com.intellij.aidebugger.evaluation.onboarding.steps.SwitchToDatasetsFolderStep
import com.intellij.aidebugger.evaluation.onboarding.steps.SwitchToEvaluationFolderStep
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EvaluationOnboardingExecutor(
    private val project: Project,
    private val steps: List<Pair<String, EvaluationOnboardingStep>>,
    private val cs: CoroutineScope,
    parentDisposable: Disposable,
    private val repeat: Boolean = false
) {
    private val disposable = Disposer.newCheckedDisposable()

    private val noOnboardingShow = setOf(CreateConfigurationFormStep.STEP_KEY, AddToDatasetDialogInputsStep.STEP_KEY)
    private val noHideOption = setOf(
        SwitchToDatasetsFolderStep.STEP_KEY,
        SwitchToEvaluationFolderStep.STEP_KEY,
        EvaluationResultsOutputStep.STEP_KEY
    )
    private val noNextOption = setOf(
        AddToDatasetButtonStep.STEP_KEY,
        EnterDatasetNameStep.STEP_KEY,
        SelectDatasetStep.STEP_KEY,
        EvaluationPlusButtonStep.STEP_KEY,
        EvaluationRunButtonStep.STEP_KEY,
        EvaluationRunCloudButtonStep.STEP_KEY
    )

    interface OnboardingUiBridge {
        fun showStep(
            stepIndex: Int,
            totalSteps: Int,
            stepId: String,
            data: EvaluationOnboardingStepData,
            controls: StepControls,
            parentDisposable: Disposable
        ): Any?

        fun dismiss(handle: Any?)
    }

    data class StepControls(
        val primaryLabel: String?,
        val onPrimary: () -> Unit,
        // Handler for auto-advance; if null, falls back to onPrimary
        val onAutoAdvance: (() -> Unit)? = null,
        val secondaryLabel: String?,
        val onSecondary: (() -> Unit)? = null,
        val onEscapeAll: (() -> Unit)? = null,
    )

    companion object {
        @Volatile
        private var uiBridge: OnboardingUiBridge? = null
        fun setUiBridge(bridge: OnboardingUiBridge) {
            uiBridge = bridge
        }
    }

    init {
        Disposer.register(parentDisposable, disposable)
    }

    suspend fun start() {
        OnboardingRuntimeFlags.onboardingActive = true
        clearOnboardingAnchorsAndFlags()
        runStep(0)
    }

    private fun finishOnboarding() {
        Disposer.dispose(disposable)
        OnboardingRuntimeFlags.onboardingActive = false
        clearOnboardingAnchorsAndFlags()
        EvaluationOnboardingService.getInstance(project).onboardingFinished()
    }

    private fun restart() {
        finishOnboarding()
        EvaluationOnboardingService.getInstance(project).startOnboarding(true)
    }

    private fun doneAll() {
        finishOnboarding()
    }

    fun resetOnboardingPassed() {
        EvaluationOnboardingService.getInstance(project).resetOnboardingPassed()
    }

    private fun nextStep(ind: Int) {
        cs.launch(Dispatchers.Main) {
            runStep(ind + 1)
        }
    }

    private suspend fun runStep(i: Int) {
        if (i >= steps.size) return

        val (stepId, step) = steps[i]

        val stepDisposable = Disposer.newCheckedDisposable()
        Disposer.register(disposable, stepDisposable)

        val data = step.performStep(project, stepDisposable)
        if (data == null) {
            Disposer.dispose(stepDisposable)
            nextStep(i)
            return
        }

        data.ensureVisible?.invoke()

        val isLast = i == steps.lastIndex
        IntroStep
        val isIntro = stepId == IntroStep.STEP_KEY
        val noSecondary = stepId in noHideOption
        val noShow = stepId in noOnboardingShow
        val nextNoShow = steps.getOrNull(i + 1)?.first in noOnboardingShow

        val primaryLabel = if (!repeat && stepId in noNextOption) null else data.onDoneActionName

        val secondaryLabel = when {
            noSecondary -> null
            isIntro -> EvaluationBundle.message("onboarding.intro.button.skip")
            isLast -> EvaluationBundle.message("onboarding.gotIt.button.repeat")
            else -> EvaluationBundle.message("onboarding.gotIt.button.hide")
        }

        var handle: Any? = null

        val controls = StepControls(
            primaryLabel = primaryLabel,
            onPrimary = createPrimaryHandler(data, stepDisposable, { handle }, isLast, i, nextNoShow),
            onAutoAdvance = createAutoAdvanceHandler(data, stepDisposable, { handle }, isLast, i),
            secondaryLabel = secondaryLabel,
            onSecondary = createSecondaryHandler({ handle }, isIntro, isLast, noSecondary),
            onEscapeAll = { doneAll() }
        )

        runCatching {
            var fired = false
            val advance: () -> Unit = {
                if (!fired) {
                    fired = true
                    cs.launch(Dispatchers.Main) {
                        (controls.onAutoAdvance ?: controls.onPrimary).invoke()
                    }
                }
            }
            data.registerAutoAdvance?.invoke(cs, stepDisposable, advance)
        }

        handle = if (noShow) null else uiBridge?.showStep(i + 1, steps.size, stepId, data, controls, stepDisposable)
    }

    private fun createPrimaryHandler(
        data: EvaluationOnboardingStepData,
        stepDisposable: Disposable,
        handleRef: () -> Any?,
        isLast: Boolean,
        index: Int,
        nextNoShow: Boolean
    ): () -> Unit = {
        Disposer.dispose(stepDisposable)
        uiBridge?.dismiss(handleRef())
        if (isLast) {
            data.onDone?.invoke()
            doneAll()
        } else {
            if (repeat && nextNoShow) nextStep(index + 1)
            else nextStep(index)
        }
    }

    private fun createAutoAdvanceHandler(
        data: EvaluationOnboardingStepData,
        stepDisposable: Disposable,
        handleRef: () -> Any?,
        isLast: Boolean,
        index: Int
    ): () -> Unit = {
        Disposer.dispose(stepDisposable)
        uiBridge?.dismiss(handleRef())
        if (isLast) {
            data.onDone?.invoke()
            doneAll()
        } else {
            nextStep(index)
        }
    }

    private fun createSecondaryHandler(
        handleRef: () -> Any?,
        isIntro: Boolean,
        isLast: Boolean,
        noSecondary: Boolean
    ): (() -> Unit)? = when {
        isIntro -> {
            { uiBridge?.dismiss(handleRef()); doneAll() }
        }

        isLast -> {
            { uiBridge?.dismiss(handleRef()); restart() }
        }

        noSecondary -> null
        else -> {
            { uiBridge?.dismiss(handleRef()); doneAll(); resetOnboardingPassed() }
        }
    }


    private fun clearOnboardingAnchorsAndFlags() {
        runCatching {
            // Flags that can cause auto-advance
            OnboardingAnchorRegistry.removeFlag(OnboardingAnchorKeys.ADD_TO_DATASET_DIALOG_CONFIRMED)
            OnboardingAnchorRegistry.removeFlag(OnboardingAnchorKeys.ENTER_DATASET_NAME_COMMITED)
            OnboardingAnchorRegistry.removeFlag(OnboardingAnchorKeys.DATASET_CREATE_STARTED)
            OnboardingAnchorRegistry.removeFlag(OnboardingAnchorKeys.EVALUATION_TOOLWINDOW_EVALUATION_FOLDER)
            OnboardingAnchorRegistry.removeFlag(OnboardingAnchorKeys.CREATE_CONFIG_CONFIRMED)
            OnboardingAnchorRegistry.removeFlag(OnboardingAnchorKeys.EVALUATION_VIEW_RUN_BUTTON_PRESSED)
            OnboardingAnchorRegistry.removeFlag(OnboardingAnchorKeys.CREATE_CONFIG_DIALOG_OPEN)
            OnboardingAnchorRegistry.removeFlag(OnboardingAnchorKeys.ADD_TO_DATASET_DIALOG_OPEN)
            OnboardingAnchorRegistry.removeFlag(OnboardingAnchorKeys.EVALUATION_VIEW_REMOTE_RUN_BUTTON_PRESSED)

            // Visual anchors that may be left after dialogs/toolwindows
            OnboardingAnchorRegistry.remove(OnboardingAnchorKeys.ADD_TO_DATASET_DIALOG)
            OnboardingAnchorRegistry.remove(OnboardingAnchorKeys.CREATE_CONFIG_DIALOG_RUN_CONFIGURATION)
            OnboardingAnchorRegistry.remove(OnboardingAnchorKeys.CREATE_CONFIG_CONFIRMED)
        }.onFailure { }
    }
}