package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.action

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSchema
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import com.intellij.bigdatatools.sparkMonitoring.icons.BigdatatoolsSparkMonitoringIcons
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilBase
import com.intellij.ui.LightweightHint
import org.jetbrains.plugins.scala.util.UnloadAwareDisposable
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.SwingUtilities

// <postStartupActivity implementation="com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.action.DfMouseHandler"/>
class DfMouseHandler : ProjectActivity {
  private var currentTooltip: MyTooltip? = null

  private val mousePressListener = object : EditorMouseListener {
    override fun mouseClicked(e: EditorMouseEvent) {
      val editor = e.editor
      val event = e.mouseEvent

      currentTooltip?.hide()

      if (!SwingUtilities.isLeftMouseButton(event)) return

      val file = PsiUtilBase.getPsiFileInEditor(editor, editor.project ?: return) ?: return

      val ref = file.findReferenceAt(editor.logicalPositionToOffset(editor.getCaretModel().logicalPosition)) ?: return

      (ref as? PsiElement)?.let {
        DfComputingUtil.getFromElement(
          DfComputingUtil.COMMON_DF_MID_TYPE_KEY,
          it
        )
      }?.let {
        currentTooltip = MyTooltip(
          editor,
          it
        ) // TooltipUI.apply(ErrorTooltip.apply("DataFrame: ${it.map}"), editor)
        currentTooltip?.myShow(editor, event.point)
      }
    }
  }

  override suspend fun execute(project: Project) {
    val multicaster = EditorFactory.getInstance().getEventMulticaster()
    multicaster.addEditorMouseListener(mousePressListener, UnloadAwareDisposable.forProject(project))
  }
}

private class MyLabel(private val editor: Editor, schema: DfTypeSchema) : JLabel(BigdatatoolsSparkMonitoringIcons.Spark) {
  init {
    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent?) {
        DfShowComputedAction.showHint(editor, schema)
      }
    })
  }
}

private class MyTooltip(editor: Editor, schema: DfTypeSchema) : LightweightHint(MyLabel(editor, schema)) {
  fun myShow(editor: Editor, point: Point) {
    val manager = HintManagerImpl.getInstanceImpl()

    fun xOnScreen(c: Component) = c.locationOnScreen.x

    val deltaX = xOnScreen(editor.getContentComponent()) - xOnScreen(editor.getContentComponent().getTopLevelAncestor())
    val x = point.x + deltaX

    val y = HintManagerImpl.getHintPosition(this, editor, editor.xyToVisualPosition(point), HintManager.ABOVE).y

    manager.showEditorHint(
      this,
      editor,
      Point(x, y),
      HintManager.HIDE_BY_ANY_KEY or HintManager.HIDE_BY_TEXT_CHANGE or HintManager.HIDE_BY_SCROLLING,
      0,
      false,
      HintManagerImpl.createHintHint(editor, Point(x, y), this, HintManager.ABOVE).setContentActive(false)
    )
  }
}
