package com.intellij.aidebugger.common.onboarding

import androidx.compose.ui.geometry.Rect

interface AnchorSink {
    fun set(key: String, rect: Rect)
    fun setFlag(key: String, isActive: Boolean)
    fun remove(key: String)
    fun removeFlag(key: String)
}

object AnchorBus {
    @Volatile
    var sink: AnchorSink? = null
}

object OnboardingAnchorKeys {
    const val THREADS_NAVIGATION_OPEN_START_ONBOARDING = "threads.navigation.view"
    const val ADD_TO_DATASET_BUTTON = "add.to.dataset.button"
    const val DATASET_CREATE_STARTED = "add.to.dataset.button"
    const val ENTER_DATASET_NAME_COMMITED = "enter.dataset.name"
    const val ADD_TO_DATASET_DIALOG = "add.to.dataset.dialog.add.button"
    const val ADD_TO_DATASET_DIALOG_OPEN = "add.to.dataset.dialog.open"
    const val ADD_TO_DATASET_DIALOG_CONFIRMED = "add.to.dataset.dialog.confirmed"

    // Toolwindow / Evaluation view
    const val EVALUATION_TOOLWINDOW_DATASETS_FOLDER = "evaluation.toolwindow.datasets.folder"
    const val EVALUATION_TOOLWINDOW_EVALUATION_FOLDER = "evaluation.toolwindow.evaluation.folder"
    const val EVALUATION_VIEW_PLUS_BUTTON = "evaluation.view.plus.button"
    const val EVALUATION_VIEW_RUN_BUTTON = "evaluation.view.run.button"
    const val EVALUATION_VIEW_RUN_BUTTON_PRESSED = "evaluation.view.run.button.pressed"
    const val EVALUATION_VIEW_REMOTE_RUN_BUTTON = "evaluation.view.remote.run.button"
    const val EVALUATION_VIEW_REMOTE_RUN_BUTTON_PRESSED = "evaluation.view.remote.run.button.pressed"
    const val EVALUATION_VIEW_RESULTS_OUTPUT_COLUMN = "evaluation.view.results.output.column"
    const val EVALUATION_VIEW_HELP_BUTTON = "evaluation.help.button"

    // Create Configuration dialog
    const val CREATE_CONFIG_DIALOG_RUN_CONFIGURATION = "create.config.dialog.run.configuration"
    const val CREATE_CONFIG_DIALOG_OPEN = "create.config.dialog.open"
    const val CREATE_CONFIG_CONFIRMED = "create.config.confirmed"
}
