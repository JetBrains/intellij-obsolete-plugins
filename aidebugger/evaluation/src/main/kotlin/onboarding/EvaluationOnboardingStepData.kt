package com.intellij.aidebugger.evaluation.onboarding

import androidx.compose.ui.geometry.Rect
import com.intellij.openapi.util.CheckedDisposable
import kotlinx.coroutines.CoroutineScope

data class EvaluationOnboardingStepData(
    val title: String? = null,
    val message: String,

    val anchorBoundsProvider: () -> Rect? = { null },

    val placement: Placement = Placement.Center,
    val offsetX: Int = 0,
    val offsetY: Int = 0,

    val highlightAnchor: Boolean = true,
    val ensureVisible: (() -> Unit)? = null,

    val beforeOnDone: (() -> Unit)? = null,
    val onDoneActionName: String,
    val onDone: (() -> Unit)? = null,
    val registerAutoAdvance: ((scope: CoroutineScope, disposable: CheckedDisposable, advance: () -> Unit) -> Unit)? = null,
)

enum class Placement {
    Above,
    Below,
    Start,
    End,
    Center
}