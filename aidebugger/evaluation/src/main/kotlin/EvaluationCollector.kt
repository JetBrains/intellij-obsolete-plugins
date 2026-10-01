package com.intellij.aidebugger.evaluation

import com.intellij.aidebugger.common.AiDebuggerPlugin
import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType
import com.intellij.aidebugger.evaluation.onboarding.EvaluationOnboardingStep
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.project.Project
import kotlin.math.roundToInt

@Suppress("UnstableApiUsage")
object EvaluationCollector : CounterUsagesCollector() {
    override fun getGroup(): EventLogGroup = GROUP

    private val GROUP = EventLogGroup("aitoolkit.evaluation", 1)

    internal const val VERSION_REGEXP = "^(?:\\d+\\.\\d+\\.\\d+|dev)$"
    internal val PLUGIN_VERSION = EventFields.StringValidatedByInlineRegexp(
        name = "plugin_version",
        regexp = VERSION_REGEXP
    )

    // region eval

    // region onboarding

    private val TOOLTIP_ID =
        EventFields.String("tooltip_id", EvaluationOnboardingStep.EP_NAME.extensionList.map { it.instance.stepId })

    // Every tooltip which is shown (template – identify by tooltip_id)
    private val tooltipShown = GROUP.registerEvent(
        eventId = "tooltip_shown",
        description = "Tooltip was shown in AI toolkit UI",
        eventField1 = TOOLTIP_ID,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportTooltipShown(
        project: Project? = null,
        tooltipId: String,
    ) {
        tooltipShown.log(
            project,
            tooltipId,
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }

    private val tooltipPrimaryButtonClicked = GROUP.registerEvent(
        eventId = "tooltip_primary_button_clicked",
        description = "Primary button of the tooltip was clicked. Usually means `Next step` or `Finish tour`",
        eventField1 = TOOLTIP_ID,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportTooltipPrimaryButtonClicked(
        project: Project? = null,
        tooltipId: String,
    ) {
        tooltipPrimaryButtonClicked.log(
            project,
            tooltipId,
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }

    private val tooltipSecondaryButtonClicked = GROUP.registerEvent(
        eventId = "tooltip_secondary_button_clicked",
        description = "Secondary button of the tooltip was clicked. Usually means `Hide`, `Skip tour`, `Repeat tour`",
        eventField1 = TOOLTIP_ID,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportTooltipSecondaryButtonClicked(
        project: Project? = null,
        tooltipId: String,
    ) {
        tooltipSecondaryButtonClicked.log(
            project,
            tooltipId,
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }

    private val tooltipEscapeAllClicked = GROUP.registerEvent(
        eventId = "tooltip_escape_all_clicked",
        description = "The escape key was clicked, closing the whole tour",
        eventField1 = TOOLTIP_ID,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportTooltipEscapeAllClicked(
        project: Project? = null,
        tooltipId: String,
    ) {
        tooltipEscapeAllClicked.log(
            project,
            tooltipId,
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }


    private val onboardingRestarted = GROUP.registerEvent(
        eventId = "onboarding_restarted",
        description = "The help button was clicked to restart the evaluation onboarding",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportOnboardingRestarted(
        project: Project? = null,
    ) {
        onboardingRestarted.log(
            project,
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }

    // endregion onboarding

    // region eval toolwindow

    private val datasetsTabSelected = GROUP.registerEvent(
        eventId = "datasets_tab_selected",
        description = "Datasets tab was selected",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportDatasetsTabSelected(project: Project? = null) {
        datasetsTabSelected.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val evaluationTabSelected = GROUP.registerEvent(
        eventId = "evaluation_tab_selected",
        description = "Evaluation tab was selected",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportEvaluationTabSelected(project: Project? = null) {
        evaluationTabSelected.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    // region datasets buttons

    private val toolwindowAddDatasetClicked = GROUP.registerEvent(
        eventId = "toolwindow_add_dataset_clicked",
        description = "Add dataset in toolwindow was clicked",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportToolwindowAddDatasetClicked(project: Project? = null) {
        toolwindowAddDatasetClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }


    private val toolwindowRemoveDatasetClicked = GROUP.registerEvent(
        eventId = "toolwindow_remove_dataset_clicked",
        description = "Remove dataset in toolwindow was clicked",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportToolwindowRemoveDatasetClicked(project: Project? = null) {
        toolwindowRemoveDatasetClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }


    private val toolwindowDuplicateDatasetClicked = GROUP.registerEvent(
        eventId = "toolwindow_duplicate_dataset_clicked",
        description = "Duplicate dataset in toolwindow was clicked",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportToolwindowDuplicateDatasetClicked(project: Project? = null) {
        toolwindowDuplicateDatasetClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }


    private val toolwindowDownloadDatasetClicked = GROUP.registerEvent(
        eventId = "toolwindow_download_dataset_clicked",
        description = "Download dataset in toolwindow was clicked",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportToolwindowDownloadDatasetClicked(project: Project? = null) {
        toolwindowDownloadDatasetClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    // endregion datasets buttons

    // region evaluation buttons

    private val toolwindowAddEvalConfigurationClicked = GROUP.registerEvent(
        eventId = "toolwindow_add_eval_configuration_clicked",
        description = "Add Eval Configuration in toolwindow was clicked",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportToolwindowAddEvalConfigurationClicked(project: Project? = null) {
        toolwindowAddEvalConfigurationClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }


    private val toolwindowRemoveEvalConfigurationClicked = GROUP.registerEvent(
        eventId = "toolwindow_remove_eval_configuration_clicked",
        description = "Remove Eval Configuration in toolwindow was clicked",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportToolwindowRemoveEvalConfigurationClicked(project: Project? = null) {
        toolwindowRemoveEvalConfigurationClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val toolwindowEditEvalConfigurationClicked = GROUP.registerEvent(
        eventId = "toolwindow_edit_eval_configuration_clicked",
        description = "Edit Eval Configuration in toolwindow was clicked",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportToolwindowEditEvalConfigurationClicked(project: Project? = null) {
        toolwindowEditEvalConfigurationClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val toolwindowDuplicateEvalConfigurationClicked = GROUP.registerEvent(
        eventId = "toolwindow_duplicate_eval_configuration_clicked",
        description = "Duplicate Eval Configuration in toolwindow was clicked",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportToolwindowDuplicateEvalConfigurationClicked(project: Project? = null) {
        toolwindowDuplicateEvalConfigurationClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val toolwindowRunEvalConfigurationClicked = GROUP.registerEvent(
        eventId = "toolwindow_run_configuration_clicked",
        description = "Run Eval Configuration in toolwindow was clicked",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportToolwindowRunEvalConfigurationClicked(project: Project? = null) {
        toolwindowRunEvalConfigurationClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val toolwindowRemoteRunEvalConfigurationClicked = GROUP.registerEvent(
        eventId = "toolwindow_remote_run_configuration_clicked",
        description = "Remote run Eval Configuration in toolwindow was clicked",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportToolwindowRemoteRunEvalConfigurationClicked(project: Project? = null) {
        toolwindowRemoteRunEvalConfigurationClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val toolwindowCancelEvalConfigurationClicked = GROUP.registerEvent(
        eventId = "toolwindow_cancel_configuration_clicked",
        description = "Cancel Eval Configuration in toolwindow was clicked",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportToolwindowCancelEvalConfigurationClicked(project: Project? = null) {
        toolwindowCancelEvalConfigurationClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    // endregion evaluation buttons

    // region eval config

    private val evalConfigLLMProviderBannerShown = GROUP.registerEvent(
        eventId = "eval_config_llm_provider_banner_shown",
        description = "Banner suggesting to add providers (possibly by importing keys) is shown in eval config dialog",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportEvalConfigLLMProviderBannerShown(project: Project? = null) {
        evalConfigLLMProviderBannerShown.log(project, AiDebuggerPlugin.PLUGIN_VERSION) }


    private val evalConfigLLMProviderBannerImportKeysButtonShown = GROUP.registerEvent(
        eventId = "eval_config_llm_provider_banner_import_keys_button_shown",
        description = "Banner suggesting to add providers has the Import keys button, i.e. API keys were found in the environment",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportEvalConfigLLMProviderBannerImportKeysButtonShown(project: Project? = null) {
        evalConfigLLMProviderBannerImportKeysButtonShown.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }


    private val evalConfigLLMProviderBannerImportKeysButtonClicked = GROUP.registerEvent(
        eventId = "eval_config_llm_provider_banner_import_keys_button_clicked",
        description = "The Import keys button in eval config banner was clicked",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportEvalConfigLLMProviderBannerImportKeysButtonClicked(project: Project? = null) {
        evalConfigLLMProviderBannerImportKeysButtonClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }


    private val evalConfigLLMProviderImportKeysSuccess = GROUP.registerEvent(
        eventId = "eval_config_llm_provider_import_keys_success",
        description = "The API keys were successfully imported via route from eval config",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportEvalConfigLLMProviderImportKeysSuccess(project: Project? = null) {
        evalConfigLLMProviderImportKeysSuccess.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }


    private val evalConfigLLMProviderBannerGoToSettingsButtonClicked = GROUP.registerEvent(
        eventId = "eval_config_llm_provider_banner_go_to_settings_button_clicked",
        description = "The Go to settings (e.g. `Add provider in settings`) button in eval config banner was clicked",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportEvalConfigLLMProviderBannerGoToSettingsButtonClicked(project: Project? = null) {
        evalConfigLLMProviderBannerGoToSettingsButtonClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val evalConfigCreated = GROUP.registerEvent(
        eventId = "eval_config_created",
        description = "The eval config was created",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportEvalConfigCreated(project: Project? = null) {
        evalConfigCreated.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    // endregion eval config

    // endregion eval toolwindow

    // region toolkit settings

    private val settingsImportKeysBannerShown = GROUP.registerEvent(
        eventId = "settings_import_keys_banner_shown",
        description = "The Import keys banner is shown in settings",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportSettingsImportKeysBannerShown(project: Project? = null) {
        settingsImportKeysBannerShown.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }


    private val settingsImportKeysBannerClicked = GROUP.registerEvent(
        eventId = "settings_import_keys_banner_clicked",
        description = "The Import keys banner is clicked in settings",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportSettingsImportKeysBannerClicked(project: Project? = null) {
        settingsImportKeysBannerClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }


    private val settingsImportKeysBannerHidden = GROUP.registerEvent(
        eventId = "settings_import_keys_banner_hidden",
        description = "The Import keys banner is hidden in settings",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportSettingsImportKeysBannerHidden(project: Project? = null) {
        settingsImportKeysBannerHidden.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }


    private val settingsImportKeysBannerClickedAndSuccess = GROUP.registerEvent(
        eventId = "settings_import_keys_banner_clicked_and_success",
        description = "The Import keys banner in settings was clicked and the keys were successfully added",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportSettingsImportKeysBannerClickedAndSuccess(project: Project? = null) {
        settingsImportKeysBannerClickedAndSuccess.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }


    private val settingsImportKeysBannerClickedAndCancelled = GROUP.registerEvent(
        eventId = "settings_import_keys_banner_clicked_and_cancelled",
        description = "The Import keys banner in settings was clicked, but then was cancelled",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportSettingsImportKeysBannerClickedAndCancelled(project: Project? = null) {
        settingsImportKeysBannerClickedAndCancelled.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }


    // endregion toolkit settings

    // eval run stats

    enum class RowCountBuckets(val range: IntRange) {
        B0(0..0),
        B1(1..5),
        B2(6..10),
        B3(11..20),
        B4(21..50),
        B5(51..100),
        B6(101..500),
        B7(501..1000),
        B_INF(1001..1001)
    }

    private val EVAL_ROWS_COUNT = EventFields.Enum("rows_count_buckets", RowCountBuckets::class.java)
    private val EVAL_EVALUATORS_COUNT = EventFields.LimitedInt("evaluators_count", 0..10)
    private val EVAL_ERROR_RATIO = EventFields.Double("ratio_of_errors_in_run", "Rounded to 0.1")

    const val UNDEFINED = "undefined"
    val modelProvider = LlmProviderType.entries.map { it.name } + UNDEFINED

    private val MODEL_PROVIDER = EventFields.String(
        "model_provider", modelProvider
    )

    private val HAS_LLM_JUDGE_EVALUATOR = EventFields.Boolean("has_llm_judge_evaluator")
    private val HAS_REGEX_EVALUATOR = EventFields.Boolean("has_regex_evaluator")

    private val evalRunFinished = GROUP.registerVarargEvent(
        eventId = "eval_run_finished",
        description = "Eval run finished",
        EVAL_ROWS_COUNT,
        EVAL_ERROR_RATIO,
        EVAL_EVALUATORS_COUNT,
        MODEL_PROVIDER,
        HAS_LLM_JUDGE_EVALUATOR,
        HAS_REGEX_EVALUATOR,
        PLUGIN_VERSION,
    )

    fun reportEvalRunFinished(
        project: Project? = null,
        rowsCount: Int,
        errorsCount: Int,
        evaluatorsCount: Int,
        modelProvider: LlmProviderType?,
        hasLLMJudge: Boolean,
        hasRegex: Boolean,
    ) {
        val rowCountBucket = RowCountBuckets.entries.firstOrNull { rowsCount in it.range } ?: RowCountBuckets.B_INF
        val evaluatorsCountCoerced = evaluatorsCount.coerceIn(0..10)
        val errorRatio = if (rowsCount == 0) {
            0.0
        } else {
            val raw = errorsCount.toDouble() / rowsCount
            (raw * 10).roundToInt() / 10.0   // round to nearest 0.1
        }
        val modelProviderName = modelProvider?.name ?: UNDEFINED
        evalRunFinished.log(
            project,
            EVAL_ROWS_COUNT.with(rowCountBucket),
            EVAL_ERROR_RATIO.with(errorRatio),
            EVAL_EVALUATORS_COUNT.with(evaluatorsCountCoerced),
            MODEL_PROVIDER.with(modelProviderName),
            HAS_LLM_JUDGE_EVALUATOR.with(hasLLMJudge),
            HAS_REGEX_EVALUATOR.with(hasRegex),
            PLUGIN_VERSION.with(AiDebuggerPlugin.PLUGIN_VERSION),
        )
    }

    // endregion eval
}