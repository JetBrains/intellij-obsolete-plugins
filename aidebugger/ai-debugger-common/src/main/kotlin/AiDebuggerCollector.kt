package com.intellij.aidebugger.common

import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.BooleanEventField
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.project.Project

object ShowReason {
    const val ALL: String = "all"
    const val AUTO: String = "auto"
}

object RunnerType {
    const val RUN: String = "run"
    const val DEBUG: String = "debug"
}

object SkipReason {
    const val NOT_SUPPORTED_EXECUTOR: String = "not_supported_executor"
    const val DISABLED: String = "disabled"
    const val NON_AI_PROJECT: String = "non_ai_project"
    const val RUN_CONFIG_IS_NULL: String = "run_config_is_null"
    const val SDK_IS_NULL: String = "sdk_is_null"
    const val PYTHON_VERSION_IS_NULL: String = "python_version_is_null"
    const val PYTHON_VERSION_IS_INVALID: String = "python_version_is_invalid"
    const val LOW_PYTHON_VERSION: String = "low_python_version"
    const val REMOTE_INTERPRETER: String = "remote_interpreter"
}

object EventType {
    const val LANGGRAPH: String = "langgraph"
    const val LANGCHAIN: String = "langchain"
    const val LANGCHAIN_EL: String = "langchain_el"
    const val KOOG: String = "koog"
    const val NULL: String = "null"
}

@Suppress("UnstableApiUsage")
object AiDebuggerCollector : CounterUsagesCollector() {
    override fun getGroup(): EventLogGroup = GROUP

    private val GROUP = EventLogGroup("aitoolkit.aiDebugger", 5)

    // region debugger

    private val TOOLWINDOW_SHOW_REASON = EventFields.String(
        "show_reason", listOf(
            ShowReason.ALL,
            ShowReason.AUTO,
        )
    )

    private val SKIP_REASON = EventFields.String(
        "skip_reason", listOf(
            SkipReason.NOT_SUPPORTED_EXECUTOR,
            SkipReason.DISABLED,
            SkipReason.NON_AI_PROJECT,
            SkipReason.RUN_CONFIG_IS_NULL,
            SkipReason.SDK_IS_NULL,
            SkipReason.PYTHON_VERSION_IS_NULL,
            SkipReason.PYTHON_VERSION_IS_INVALID,
            SkipReason.LOW_PYTHON_VERSION,
            SkipReason.REMOTE_INTERPRETER,
        )
    )
    
    private const val FRAMEWORK_NULL = "Null"

    private val frameworkNames = Framework.entries.map { it.name } + FRAMEWORK_NULL

    private val EVENT_TYPE = EventFields.String(
        "event_type", frameworkNames
    )

    private fun Framework?.toFusString(): String = this?.name ?: FRAMEWORK_NULL

    internal const val VERSION_REGEXP = "^(?:\\d+\\.\\d+\\.\\d+|dev)$"
    internal val PLUGIN_VERSION = EventFields.StringValidatedByInlineRegexp(
        name = "plugin_version",
        regexp = VERSION_REGEXP
    )

    val allowedExecutors = setOf(
        RunnerType.RUN,
        RunnerType.DEBUG,
    )
    private val CUSTOM_RUNNER_TYPE = EventFields.String("custom_runner_type", allowedExecutors.toList())

    private val EVENTS_COUNT = EventFields.Int("events_count")
    private val THREADS_COUNT = EventFields.Int("threads_count")

    private val aiDebuggerDisable = GROUP.registerEvent(
        eventId = "debugger_disable",
        description = "User disabled AI Debugger",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportAiDebuggerDisable(project: Project? = null) {
        aiDebuggerDisable.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    val aiDebuggerEnable = GROUP.registerEvent(
        eventId = "debugger_enable",
        description = "User enabled AI Debugger",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportAiDebuggerEnable(project: Project? = null) {
        aiDebuggerEnable.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val disableAutoShow = GROUP.registerEvent(
        eventId = "disable_auto_show",
        description = "Don't show AI Debugger automatically",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportDisableAutoShow(project: Project? = null) {
        disableAutoShow.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val toolWindowShow = GROUP.registerVarargEvent(
        eventId = "tool_window_show",
        description = "AI Debugger tool window was opened (by user, or auto show on run)",
        EVENTS_COUNT,
        THREADS_COUNT,
        PLUGIN_VERSION,
        TOOLWINDOW_SHOW_REASON,
    )

    fun reportToolWindowShown(project: Project?, eventsCount: Int, threadsCount: Int, reason: String): Unit =
        toolWindowShow.log(
            project,
            EVENTS_COUNT.with(eventsCount),
            THREADS_COUNT.with(threadsCount),
            PLUGIN_VERSION.with(AiDebuggerPlugin.PLUGIN_VERSION),
            TOOLWINDOW_SHOW_REASON.with(reason)
        )

    private val toolWindowClose = GROUP.registerVarargEvent(
        eventId = "tool_window_close",
        description = "AI Debugger tool window was closed by user",
        EVENTS_COUNT,
        THREADS_COUNT,
        PLUGIN_VERSION,
    )

    fun reportToolWindowClosed(project: Project?, eventsCount: Int, threadsCount: Int): Unit =
        toolWindowClose.log(
            project,
            EVENTS_COUNT.with(eventsCount),
            THREADS_COUNT.with(threadsCount),
            PLUGIN_VERSION.with(AiDebuggerPlugin.PLUGIN_VERSION)
        )

    private val graphShow = GROUP.registerEvent(
        eventId = "graph_show",
        description = "AI Debugger show graph has pressed",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportGraphShown(project: Project? = null) {
        graphShow.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val threadShow = GROUP.registerEvent(
        eventId = "thread_show",
        description = "AI Debugger show thread has pressed",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportThreadShown(project: Project? = null) {
        threadShow.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val rawShow = GROUP.registerEvent(
        eventId = "raw_show",
        description = "AI Debugger show raw view has pressed",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportRawShown(project: Project? = null) {
        rawShow.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val prettyShow = GROUP.registerEvent(
        eventId = "pretty_show",
        description = "AI Debugger show pretty view has pressed",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportPrettyShown(project: Project? = null) {
        prettyShow.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val customRunnerSelected = GROUP.registerEvent(
        eventId = "custom_runner_selected",
        description = "AI Debugger Custom Runner was selected to run the configuration",
        eventField1 = CUSTOM_RUNNER_TYPE,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportCustomRunnerSelected(project: Project? = null, runnerType: String) {
        customRunnerSelected.log(
            project,
            runnerType.lowercase(),
            AiDebuggerPlugin.PLUGIN_VERSION
        )
    }

    private val customRunnerSkipped = GROUP.registerEvent(
        eventId = "custom_runner_skipped",
        description = "AI Debugger Custom Runner was skipped while plugin is installed",
        eventField1 = SKIP_REASON,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportCustomRunnerSkipped(project: Project? = null, skipReason: String) {
        customRunnerSkipped.log(
            project,
            skipReason,
            AiDebuggerPlugin.PLUGIN_VERSION
        )
    }

    private val noPrettyViewFound = GROUP.registerEvent(
        eventId = "no_pretty_view_found",
        description = "No pretty view found for the event",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportNoPrettyViewFound(project: Project? = null) {
        noPrettyViewFound.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val threadFinished = GROUP.registerEvent(
        eventId = "thread_finished",
        description = "Thread finished",
        eventField1 = EVENTS_COUNT,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportThreadFinished(project: Project? = null, eventsCount: Int) {
        threadFinished.log(
            project,
            eventsCount,
            AiDebuggerPlugin.PLUGIN_VERSION
        )
    }

    private val sessionFinished = GROUP.registerEvent(
        eventId = "session_finished",
        description = "Session finished",
        eventField1 = THREADS_COUNT,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportSessionFinished(project: Project? = null, threadsCount: Int) {
        sessionFinished.log(
            project,
            threadsCount,
            AiDebuggerPlugin.PLUGIN_VERSION
        )
    }

    private val issueClick = GROUP.registerEvent(
        eventId = "report_issue_click",
        eventField1 = PLUGIN_VERSION,
        description = "Report Issue button was clicked"
    )

    fun reportIssueClick(project: Project? = null) {
        issueClick.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val runtimeRequirementsNotMet = GROUP.registerEvent(
        eventId = "runtime_requirements_not_met",
        description = "User's process sent signal that requirements are not met (e.g. langgraph not found)",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportRuntimeRequirementsNotMet(project: Project? = null) {
        runtimeRequirementsNotMet.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    private val firstEventReceived = GROUP.registerEvent(
        eventId = "first_event_received",
        description = "First event from the user's process received",
        eventField1 = PLUGIN_VERSION,
        eventField2 = EVENT_TYPE,
    )
    fun reportFirstEventReceived(project: Project? = null, eventType: Framework?) {
        firstEventReceived.log(project, AiDebuggerPlugin.PLUGIN_VERSION, eventType.toFusString())
    }

    private val buyPycharmProClicked = GROUP.registerEvent(
        eventId = "buy_pycharm_pro_clicked",
        description = "Buy PyCharm Pro button was clicked after showing paywall",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportBuyPycharmProClicked(project: Project? = null) {
        buyPycharmProClicked.log(project, AiDebuggerPlugin.PLUGIN_VERSION)
    }

    // endregion debugger

    // region eval
    // parts of eval which are defined in the common module

    // region add to dataset

    // Add to dataset shown
    private val addToDatasetShown = GROUP.registerEvent(
        eventId = "add_to_dataset_shown",
        description = "\"Add to dataset\" was shown",
        eventField1 = EVENT_TYPE,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportAddToDatasetShown(project: Project? = null, traceType: Framework? = null) {
        addToDatasetShown.log(project, traceType.toFusString(), AiDebuggerPlugin.PLUGIN_VERSION)
    }

    // Add to dataset clickable
    private val addToDatasetBecameClickable = GROUP.registerEvent(
        eventId = "add_to_dataset_became_clickable",
        description = "\"Add to dataset\" became clickable",
        eventField1 = EVENT_TYPE,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportAddToDatasetBecameClickable(project: Project? = null, traceType: Framework? = null) {
        addToDatasetBecameClickable.log(project, traceType.toFusString(), AiDebuggerPlugin.PLUGIN_VERSION)
    }

    // Add to dataset clicked (+ type of trace)
    private val addToDatasetClicked = GROUP.registerEvent(
        eventId = "add_to_dataset_clicked",
        description = "\"Add to dataset\" was clicked",
        eventField1 = EVENT_TYPE,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportAddToDatasetClicked(
        project: Project? = null,
        traceType: Framework? = null,
    ) {
        addToDatasetClicked.log(
            project,
            traceType.toFusString(),
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }

    // region add to dataset popup

    // Add to dataset popup was shown
    private val addToDatasetPopupShown = GROUP.registerEvent(
        eventId = "add_to_dataset_popup_shown",
        description = "Add to dataset popup shown",
        eventField1 = EVENT_TYPE,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportAddToDatasetPopupShown(
        project: Project? = null,
        traceType: Framework? = null,
    ) {
        addToDatasetPopupShown.log(
            project,
            traceType.toFusString(),
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }

    private val addToDatasetPopupNewDatasetCreationStarted = GROUP.registerEvent(
        eventId = "add_to_dataset_popup_new_dataset_creation_started",
        description = "Add to dataset popup shown",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportAddToDatasetPopupNewDatasetCreationStarted(
        project: Project? = null,
    ) {
        addToDatasetPopupNewDatasetCreationStarted.log(
            project,
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }


    private val addToDatasetPopupNewDatasetCreationCancelled = GROUP.registerEvent(
        eventId = "add_to_dataset_popup_new_dataset_creation_cancelled",
        description = "Add to dataset popup shown",
        eventField1 = PLUGIN_VERSION,
    )

    fun reportAddToDatasetPopupNewDatasetCreationCancelled(
        project: Project? = null,
    ) {
        addToDatasetPopupNewDatasetCreationCancelled.log(
            project,
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }

    // Add to dataset popup checkbox was clicked
    private val addToDatasetPopupCheckboxClicked = GROUP.registerEvent(
        eventId = "add_to_dataset_popup_checkbox_clicked",
        description = "Add to dataset popup shown",
        eventField1 = EVENT_TYPE,
        eventField2 = BooleanEventField("add_to_dataset_popup_checkbox_checked"),
        eventField3 = PLUGIN_VERSION,
    )

    fun reportAddToDatasetPopupCheckboxClicked(
        project: Project? = null,
        traceType: Framework? = null,
        checked: Boolean,
    ) {
        addToDatasetPopupCheckboxClicked.log(
            project,
            traceType.toFusString(),
            checked,
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }

    // endregion add to dataset popup

    // region add trace popup


    // Add to dataset popup was shown
    private val addTracePopupShown = GROUP.registerEvent(
        eventId = "add_trace_popup_shown",
        description = "Add trace popup shown",
        eventField1 = EVENT_TYPE,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportAddTracePopupShown(
        project: Project? = null,
        traceType: Framework? = null,
    ) {
        addTracePopupShown.log(
            project,
            traceType.toFusString(),
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }

    private val defaultInputJsonPathCorrectness = GROUP.registerEvent(
        eventId = "default_input_json_path_correctness",
        description = "Default input json path correctness when showing Add trace popup",
        eventField1 = EVENT_TYPE,
        eventField2 = BooleanEventField("default_input_json_path_correctness_value"),
        eventField3 = PLUGIN_VERSION,
    )

    fun reportDefaultInputJsonPathCorrectness(
        project: Project? = null,
        traceType: Framework? = null,
        correctness: Boolean,
    ) {
        defaultInputJsonPathCorrectness.log(
            project,
            traceType.toFusString(),
            correctness,
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }


    private val defaultExpectedJsonPathCorrectness = GROUP.registerEvent(
        eventId = "default_expected_json_path_correctness",
        description = "Default expected json path correctness when showing Add trace popup",
        eventField1 = EVENT_TYPE,
        eventField2 = BooleanEventField("default_expected_json_path_correctness_value"),
        eventField3 = PLUGIN_VERSION,
    )

    fun reportDefaultExpectedJsonPathCorrectness(
        project: Project? = null,
        traceType: Framework? = null,
        correctness: Boolean,
    ) {
        defaultExpectedJsonPathCorrectness.log(
            project,
            traceType.toFusString(),
            correctness,
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }

    // endregion add trace popup

    // Add to dataset was successful
    private val createdNewDatasetFromPopup = GROUP.registerEvent(
        eventId = "created_new_dataset_from_popup",
        description = "Dataset was created using 'New' field in Add to dataset popup",
        eventField1 = EVENT_TYPE,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportCreatedNewDatasetFromPopup(
        project: Project? = null,
        traceType: Framework? = null,
    ) {
        createdNewDatasetFromPopup.log(
            project,
            traceType.toFusString(),
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }

    // Add to dataset was successful
    private val addToExistingDatasetFromPopupSuccess = GROUP.registerEvent(
        eventId = "add_to_existing_dataset_from_popup_success",
        description = "Trace was successfully added to dataset",
        eventField1 = EVENT_TYPE,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportAddToExistingDatasetFromPopupSuccess(
        project: Project? = null,
        traceType: Framework? = null,
    ) {
        addToExistingDatasetFromPopupSuccess.log(
            project,
            traceType.toFusString(),
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }

    // Add to dataset was successful
    private val addToExistingDatasetFromPopupCancelled = GROUP.registerEvent(
        eventId = "add_to_existing_dataset_from_popup_cancelled",
        description = "Trace was failed to add to dataset",
        eventField1 = EVENT_TYPE,
        eventField2 = PLUGIN_VERSION,
    )

    fun reportAddToExistingDatasetFromPopupCancelled(
        project: Project? = null,
        traceType: Framework? = null,
    ) {
        addToExistingDatasetFromPopupCancelled.log(
            project,
            traceType.toFusString(),
            AiDebuggerPlugin.PLUGIN_VERSION,
        )
    }

    // endregion add to dataset

    // endregion eval
}