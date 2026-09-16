package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.util.ui.NamedColorUtil
import java.awt.AlphaComposite
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseEvent
import javax.swing.JComponent

private class FadingActionToolbarImpl(actionGroup: ActionGroup) : ActionToolbarImpl(ActionPlaces.CONTEXT_TOOLBAR, actionGroup, true) {
  init {
    setMinimumButtonSize(Dimension(22, 22))
    setSkipWindowAdjustments(true)
    setReservePlaceAutoPopupIcon(false)
    isOpaque = false
  }

  override fun fillToolBar(actions: List<AnAction>, layoutSecondaries: Boolean) {
    super.fillToolBar(actions, layoutSecondaries)
    parent?.let {
      it.setBounds(it.x + it.size.width - it.preferredSize.width, it.y, it.preferredSize.width, it.height)
    }
  }
}

class FadingToolbar(actionGroup: ActionGroup, targetComponent: JComponent) : FadingPanel() {

  init {
    val actionToolbar = FadingActionToolbarImpl(actionGroup)
    actionToolbar.targetComponent = targetComponent

    layout = BorderLayout()
    add(actionToolbar.component, BorderLayout.CENTER)

    cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)

    enableEvents(MouseEvent.MOUSE_EVENT_MASK or MouseEvent.MOUSE_MOTION_EVENT_MASK)
  }

  override fun paintComponent(g: Graphics) {
    val graphics = g.create()
    try {
      if (graphics is Graphics2D) {
        graphics.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      }

      graphics.color = EditorColorsManager.getInstance().globalScheme.defaultBackground
      graphics.fillRoundRect(0, 0, bounds.width, bounds.height, height, height)

      graphics.color = NamedColorUtil.getBoundsColor()
      graphics.drawRoundRect(0, 0, bounds.width - 1, bounds.height - 1, height, height)

      super.paintComponent(graphics)
    }
    finally {
      graphics.dispose()
    }
  }
}