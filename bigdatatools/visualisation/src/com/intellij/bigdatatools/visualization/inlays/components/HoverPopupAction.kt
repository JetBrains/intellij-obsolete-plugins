package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsActions
import com.intellij.util.ui.JBUI
import javax.swing.Icon
import javax.swing.JComponent

/**
 * Creates a default ActionButton for toolbar and adds a HoverPopup with supplied actions
 * @see HoverPopup
 */
open class HoverPopupAction(@NlsActions.ActionText text: String?,
                            @NlsActions.ActionDescription description: String?,
                            icon: Icon?,
                            private val actions: List<AnAction>) : DumbAwareAction(text, description, icon), CustomComponentAction {

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
    val button = ActionButton(this, null, "HoverPopup", JBUI.emptySize().size())
    HoverPopup.installOn(button, actions)
    return button
  }

  override fun actionPerformed(e: AnActionEvent) {}
}