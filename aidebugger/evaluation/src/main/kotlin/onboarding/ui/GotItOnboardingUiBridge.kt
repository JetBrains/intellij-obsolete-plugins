package com.intellij.aidebugger.evaluation.onboarding.ui

import androidx.compose.ui.geometry.Rect
import com.intellij.aidebugger.evaluation.EvaluationCollector
import com.intellij.aidebugger.evaluation.onboarding.EvaluationOnboardingExecutor
import com.intellij.aidebugger.evaluation.onboarding.EvaluationOnboardingStepData
import com.intellij.aidebugger.evaluation.onboarding.Placement
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import javax.swing.JComponent
import javax.swing.RootPaneContainer
import javax.swing.SwingUtilities

class GotItOnboardingUiBridge(private val project: Project) : EvaluationOnboardingExecutor.OnboardingUiBridge {

    private data class Handle(val dismiss: () -> Unit)

    override fun showStep(
        stepIndex: Int,
        totalSteps: Int,
        stepId: String,
        data: EvaluationOnboardingStepData,
        controls: EvaluationOnboardingExecutor.StepControls,
        parentDisposable: Disposable
    ): Any? {
        EvaluationCollector.reportTooltipShown(project, stepId)
        val owner = findBalloonOwner() ?: return null

        val anchorRect = data.anchorBoundsProvider.invoke()
        val showInCenter = (anchorRect == null) || data.placement == Placement.Center

        val builder = GotItComponentBuilder { data.message }
        if (data.title != null) builder.withHeader(data.title)

        builder
            .onEscapePressed {
                EvaluationCollector.reportTooltipEscapeAllClicked(project, stepId)
                controls.onEscapeAll?.invoke()
            }
            .requestFocus(true)

        if (controls.primaryLabel != null) {
            builder
                .withButtonLabel(controls.primaryLabel)
                .onButtonClick {
                    EvaluationCollector.reportTooltipPrimaryButtonClicked(project, stepId)
                    controls.onPrimary.invoke()
                }
        } else if (controls.secondaryLabel != null && controls.onSecondary != null) {
            builder
                .withButtonLabel(controls.secondaryLabel)
                .onButtonClick {
                    EvaluationCollector.reportTooltipSecondaryButtonClicked(project, stepId)
                    controls.onSecondary.invoke()
                }
        }
        if (controls.primaryLabel != null && controls.secondaryLabel != null && controls.onSecondary != null) {
            builder.withSecondaryButton(controls.secondaryLabel) {
                EvaluationCollector.reportTooltipSecondaryButtonClicked(project, stepId)
                controls.onSecondary.invoke()
            }
        }

        val balloonDisposable = Disposer.newCheckedDisposable()
        Disposer.register(project, balloonDisposable) // <-- register under project so it's automatically cleaned

        val balloon: Balloon = builder.build(parentDisposable) {
            setShowCallout(!showInCenter)
        }

        if (showInCenter) {
            val center = Point(owner.width / 2, owner.height / 2)
            val rp = RelativePoint(owner, center)
            balloon.show(rp, Balloon.Position.above)
            return Handle { balloon.hide() }
        }

        val rp = toRelativePoint(owner, anchorRect!!, data)
        val pos = toBalloonPosition(data.placement)
        balloon.show(rp, pos)
        return Handle { balloon.hide() }
    }

    override fun dismiss(handle: Any?) {
        val h = handle as? Handle ?: return
        h.dismiss.invoke()
    }

    private fun toRelativePoint(owner: JComponent, r: Rect, data: EvaluationOnboardingStepData): RelativePoint {
        val x = when (data.placement) {
            Placement.Above, Placement.Below, Placement.Center -> (r.left + r.right) / 2f
            Placement.Start -> r.left
            Placement.End -> r.right
        }
        val y = when (data.placement) {
            Placement.Above -> r.top
            Placement.Below -> r.bottom
            Placement.Start, Placement.End, Placement.Center -> (r.top + r.bottom) / 2f
        }

        val ptScreen = Point(x.toInt() + data.offsetX, y.toInt() + data.offsetY)
        SwingUtilities.convertPointFromScreen(ptScreen, owner)
        return RelativePoint(owner, ptScreen)
    }

    private fun toBalloonPosition(placement: Placement): Balloon.Position = when (placement) {
        Placement.Above -> Balloon.Position.above
        Placement.Below -> Balloon.Position.below
        Placement.Start -> Balloon.Position.atLeft
        Placement.End -> Balloon.Position.atRight
        Placement.Center -> Balloon.Position.above // won't be used when centered
    }

    /**
     * - If a modal dialog is focused, attach to its GLASS PANE so the balloon
     *   sits above the dialog’s own glass and receives clicks within the dialog’s modality scope.
     * - Otherwise, attach to the IDE frame component.
     */
    private fun findBalloonOwner(): JComponent? {
        val kb = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val active = kb.activeWindow ?: kb.focusedWindow
        // Prefer the glassPane when a dialog/window is active
        val rpc = (active as? RootPaneContainer)
        val glass = rpc?.glassPane as? JComponent
        if (glass != null && glass.isShowing) return glass
        // Fallback to layeredPane
        val layered = rpc?.layeredPane as? JComponent
        if (layered != null && layered.isShowing) return layered

        return WindowManager.getInstance().getIdeFrame(project)?.component
    }
}
