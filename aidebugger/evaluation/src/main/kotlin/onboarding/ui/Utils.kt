package com.intellij.aidebugger.evaluation.onboarding.ui

import androidx.compose.ui.geometry.Rect
import com.intellij.aidebugger.evaluation.EvaluationBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.ui.UIBundle
import java.awt.Component
import java.awt.Container
import java.awt.Point
import javax.swing.JTable
import javax.swing.SwingUtilities

fun componentToRect(component: Component): Rect {
    val pt = Point(0, 0)
    SwingUtilities.convertPointToScreen(pt, component)
    val b = component.bounds
    return Rect(pt.x.toFloat(), pt.y.toFloat(), (pt.x + b.width).toFloat(), (pt.y + b.height).toFloat())
}

fun findPlusButton(parent: Component): ActionButton? =
    findActionButton(parent) { btn ->
        val t = btn.presentation.text ?: btn.action.templatePresentation.text
        t?.contains(UIBundle.message("button.text.add"), ignoreCase = true) == true
    }

fun findRunButton(parent: Component): ActionButton? =
    findActionButton(parent) { btn ->
        val t = btn.presentation.text ?: btn.action.templatePresentation.text
        val icon = btn.presentation.icon
        t?.contains(EvaluationBundle.message("eval.view.button.run"), ignoreCase = true) == true ||
                icon === AllIcons.Actions.Execute
    }

fun findRemoteRunButton(parent: Component): ActionButton? =
    findActionButton(parent) { btn ->
        val t = btn.presentation.text ?: btn.action.templatePresentation.text
        t?.contains(EvaluationBundle.message("eval.view.button.remote.run"), ignoreCase = true) == true
    }

private fun findActionButton(root: Component?, predicate: (ActionButton) -> Boolean): ActionButton? {
    root ?: return null

    return when (root) {
        is ActionButton -> if (predicate(root)) root else null
        is Container -> (0 until root.componentCount)
            .asSequence()
            .mapNotNull { findActionButton(root.getComponent(it), predicate) }
            .firstOrNull()

        else -> null
    }
}

fun findActionToolbarContainer(root: Component): Component? {
    if (root.javaClass.name.contains("ActionToolbar", ignoreCase = true)) return root
    if (root !is Container) return null

    return (0 until root.componentCount)
        .asSequence()
        .mapNotNull { findActionToolbarContainer(root.getComponent(it)) }
        .firstOrNull()
}

inline fun <T> ignoreErrors(block: () -> T): T? =
    try {
        block()
    } catch (_: Throwable) {
        null
    }

fun Component.screenRectOrNull(): Rect? = ignoreErrors {
    if (!isShowing) return null
    val p = Point(0, 0)
    SwingUtilities.convertPointToScreen(p, this)
    Rect(
        p.x.toFloat(),
        p.y.toFloat(),
        (p.x + width).toFloat(),
        (p.y + height).toFloat()
    )
}

fun fallbackRect(base: Component): Rect? = base.screenRectOrNull()

fun JTable.headerRectForColumn(name: String): Rect? = ignoreErrors {
    val header = tableHeader ?: return null
    val index = (0 until columnModel.columnCount)
        .firstOrNull { i -> columnModel.getColumn(i).headerValue?.toString() == name }
        ?: return null

    val cell = header.getHeaderRect(index)
    val p = header.locationOnScreen
    Rect(
        (p.x + cell.x).toFloat(),
        (p.y + cell.y).toFloat(),
        (p.x + cell.x + cell.width).toFloat(),
        (p.y + cell.y + cell.height).toFloat()
    )
}

fun getEvaluationTabRect(component: Component): Rect? {
    val root = findToolWindowTabsHeaderContainer(component)
    var candidate: Component? = null
    try {
        val targetGuess = root?.components?.getOrNull(2)
        if (targetGuess != null) candidate = targetGuess
    } catch (_: Throwable) {
        return null
    }
    if (candidate == null) return null
    return componentToRect(candidate)
}

fun getDatasetsTabRect(component: Component): Rect? {
    val root = findToolWindowTabsHeaderContainer(component)
    var candidate: Component? = null
    try {
        val targetGuess = root?.components?.getOrNull(1)
        if (targetGuess != null) candidate = targetGuess
    } catch (_: Throwable) {
        return null
    }
    if (candidate == null) return null
    return componentToRect(candidate)
}


private fun findToolWindowTabsHeaderContainer(contentComponent: Component): Container? {
    return try {
        var root: Container? = contentComponent.parent
        repeat(5) { if (root is Container) root = root.components[0] as Container }
        return root
    } catch (_: Throwable) {
        null
    }
}

fun findHelpButton(contentComponent: Component): ActionButton? {
    return try {
        var root: Container? = contentComponent.parent
        repeat(3) { if (root is Container) root = root.components[0] as Container }
        val wrapper = (root as Container).components[1] as Container
        val candidate = (wrapper.components[0] as Container).components.find {
            it is ActionButton && (it.presentation.text == EvaluationBundle.message("eval.view.help.button.tooltip") ||
                    it.presentation.text == EvaluationBundle.message("eval.view.help.button.tooltip.disabled"))
        } // help button is the first one
        return candidate as ActionButton?
    } catch (_: Throwable) {
        null
    }
}