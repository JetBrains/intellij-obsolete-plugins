package com.intellij.bigdatatools.visualization.inlays

import com.google.gson.JsonObject
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.visualization.inlays.components.GlassPanePanel
import com.intellij.bigdatatools.visualization.inlays.style.HighlightMode
import com.intellij.bigdatatools.visualization.inlays.style.InlaysConfig
import com.intellij.bigdatatools.visualization.inlays.utils.use
import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.charts.dataframe.DataFrameKeys.NOTEBOOK_INLAY_NAME_KEY
import com.intellij.charts.utils.asJsonObjectOrNull
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.LineMarkerRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.util.TextRange
import com.intellij.ui.ExperimentalUI
import com.intellij.ui.PopupHandler
import com.intellij.ui.scale.JBUIScale
import com.intellij.ui.util.minimumHeight
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicLong
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.math.max

class NotebookInlayComponent(
  val editor: EditorImpl,
  val cell: NotebookCell,
  editorLastScrollTimestamp: AtomicLong,
) : InlayComponent(), UiDataProvider {
  companion object {
    val NOTEBOOK_INLAY_COMPONENT_KEY: DataKey<NotebookInlayComponent> = DataKey.create("NOTEBOOK_INLAY_COMPONENT_KEY")
    private val logger = Logger.getInstance(this::class.java)

    val stripeWidth: Int
      get() = if (ExperimentalUI.isNewUI()) JBUIScale.scale(2) else JBUIScale.scale(4)

    val stripeOffset: Int
      get() = if (ExperimentalUI.isNewUI()) JBUIScale.scale(2) else JBUIScale.scale(6)

    private val globalHighlighterRenderer = NotebookCellHighlighterRenderer()
  }

  /** Invoked on any settings change (size or series settings).*/
  var onChange: (() -> Unit)? = null

  /** Called on any height change. */
  var onHeightChanged: ((userSized: Boolean) -> Unit)? = null

  var expandedHeight: Int = 0

  var collapsed: Boolean = false
  var collapsible: Boolean = false

  /** Flag is set when user changes she size. If set, autoresize and wit height disabled. Flag is saved. */
  var userSized: Boolean = false
    set(value) {
      if (field != value) {
        field = value
        onChange?.invoke()
      }
    }

  private var gutterComponent: NotebookInlayComponentGutter? = null

  private var gutter: JComponent? = null

  /** Draws bottom separator line and selection background in gutter area. */
  //private var cellHighlighter: RangeHighlighter? = null

  /** Draws cell selected background. */
  private var selectionHighlighter: RangeHighlighter? = null

  val statusController: NotebookInlayStatusController = NotebookInlayStatusController(this)
  val outputController: NotebookInlayOutputController = NotebookInlayOutputController(this)
  val toolbarController: NotebookInlayToolbarController = NotebookInlayToolbarController(this)
  val resizeController: InlayResizeController = InlayResizeController(this)

  /**
   * Event from notebook with new status. Status can be: UNKNOWN, READY, PENDING, RUNNING, FINISHED, ERROR, ABORT.
   * In normal case statuses will come in this sequence: READY - PENDING - RUNNING - FINISHED
   * In error case: READY - PENDING - ERROR - ERROR
   */
  var status: CellStatus = CellStatus.UNKNOWN
    set(value) {

      when (value) {
        CellStatus.READY -> {
          if (field != CellStatus.UNKNOWN) {
            //outputController.getCurrentPage()?.let {
            //  VisualizationChangesCollector.visualizationRunEvent.log(editor.project!!, it)
            //}
            clearOnReady()
          }
        }
        CellStatus.PENDING -> {
          toolbarController.setDescription(value.text)
          toolbarController.showProgress()
        }
        CellStatus.RUNNING -> toolbarController.showProgress()
        CellStatus.FINISHED, CellStatus.ERROR -> toolbarController.hide()
        CellStatus.ABORT -> {
          toolbarController.hideProgress()
          toolbarController.setDescription(value.text)
        }
        CellStatus.UNKNOWN -> Unit
      }

      field = value

      if (cell.output == null || cell.output?.msg?.isEmpty() == true) {
        toolbarController.setDescription(value.text)
      }
    }

  val cellTextRange: TextRange
    get() = cell.textRange

  var textLinesRange: TextRange

  val top: JPanel = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    isOpaque = false
  }

  val bottom: JPanel = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    isOpaque = false
  }

  /** Main panel where all inlay components lives. */
  val panel: JPanel = JPanel(BorderLayout()).apply {
    isOpaque = false
    add(top, BorderLayout.NORTH)
    add(bottom, BorderLayout.SOUTH)
  }

  /** Glass pane, handle mouse wheel scroll. */
  private val transparentPanel = GlassPanePanel(editorLastScrollTimestamp)

  fun hasOutput(): Boolean = outputController.hasOutput()

  var mouseOverInlay: Boolean = false
    set(value) {
      if (field == value) {
        return
      }
      field = value
      outputController.showToolbar = value || selected
    }

  var synced: Boolean = true
    set(value) {
      if (field != value) {
        field = value
        (editor.gutter as JComponent).repaint()
      }
    }

  var selected: Boolean = false
    set(value) {

      if (field == value) {
        return
      }

      field = value

      outputController.showToolbar = value || mouseOverInlay

      if (InlaysConfig.getInstance().cellHighlightMode == HighlightMode.NONE) {
        removeSelectionHighlighter()
      }
      else if (InlaysConfig.getInstance().cellHighlightMode == HighlightMode.ALL) {
        if (selectionHighlighter == null) {
          updateSelectionHighlighter()
        }
      }
      else {
        if (value && selectionHighlighter == null) {
          updateSelectionHighlighter()
        }
        else if (!value) {
          removeSelectionHighlighter()
        }
      }

      // To repaint vertical line in IDE gutter, drawn by cellHighlighter.
      (editor.gutter as JComponent).repaint()
    }

  init {
    cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
    border = JBUI.Borders.empty(InlayDimensions.topBorderUnscaled,
                                InlayDimensions.leftBorderUnscaled,
                                InlayDimensions.bottomBorderUnscaled,
                                InlayDimensions.rightBorderUnscaled)

    add(panel)
    setLayer(panel, DEFAULT_LAYER)

    transparentPanel.addWheelListener(panel, editor.contentComponent)
    add(transparentPanel)
    setLayer(transparentPanel, DEFAULT_LAYER + 1)

    if (InlaysConfig.getInstance().cellHighlightMode == HighlightMode.ALL && selectionHighlighter == null) {
      updateSelectionHighlighter()
    }

    addMouseListener(object : PopupHandler() {
      override fun invokePopup(comp: Component, x: Int, y: Int) {

        val showBottomInfoAction = object : DumbAwareToggleAction(VisMessagesBundle.message("style.action.showBottomInfo"), null, null) {
          override fun isSelected(e: AnActionEvent) = InlaysConfig.getInstance().showBottomInfo
          override fun getActionUpdateThread() = ActionUpdateThread.BGT
          override fun setSelected(e: AnActionEvent, state: Boolean) {
            if (InlaysConfig.getInstance().showBottomInfo != state) {
              InlaysConfig.getInstance().showBottomInfo = state
              InlaysConfig.getInstance().notifyChange()
            }
          }
        }

        ActionManager.getInstance().createActionPopupMenu(ActionPlaces.EDITOR_POPUP,
                                                          DefaultActionGroup(
                                                            Separator.create(VisMessagesBundle.message("style.action.title")),
                                                            showBottomInfoAction
                                                          )).component.show(comp, x, y)
      }
    })

    val textRange = NotebookEditorUtils.getCellRangeInEditor(editor, cell)
    textLinesRange = TextRange(editor.offsetToVisualLine(textRange.startOffset), editor.offsetToVisualLine(textRange.endOffset))
  }

  override fun uiDataSnapshot(sink: DataSink) {
    sink[NOTEBOOK_INLAY_COMPONENT_KEY] = this
    sink[NOTEBOOK_INLAY_NAME_KEY] =
      if (cell.titleVisible && !cell.title.isNullOrBlank()) cell.title ?: ""
      else "cell${cell.indexInNote.let { " $it" }}"
  }

  /** @force flag we need when we are updating cells backgrounds due to the color scheme change. */
  fun updateSelectionHighlighter(force: Boolean = false) {

    if (InlaysConfig.getInstance().cellHighlightMode == HighlightMode.NONE) {
      selectionHighlighter?.let { editor.markupModel.removeHighlighter(it) }
      return
    }

    val textRange = NotebookEditorUtils.getCellRangeInEditor(editor, cell)

    // ToDo This is half-hack. The problem is that updateCellSeparator called from PSI change
    // but if we have a lot of sequential changes in editor.document (line Backspace button is hold)
    // document was updated but PSI update is async and that's why we have this check.
    if (textRange.endOffset > editor.document.textLength && !force) {
      return
    }

    val selectionHighlighter = selectionHighlighter

    selectionHighlighter?.let { editor.markupModel.removeHighlighter(it) }

    try {
      // Highlighter, highlights cell text.
      this.selectionHighlighter = editor.markupModel.addRangeHighlighter(textRange.startOffset,
                                                                         textRange.endOffset,
                                                                         HighlighterLayer.ADDITIONAL_SYNTAX + 1,
                                                                         TextAttributes(null, getCodeCellBackground(editor.colorsScheme),
                                                                                        null, EffectType.ROUNDED_BOX, Font.PLAIN),
                                                                         HighlighterTargetArea.LINES_IN_RANGE).apply {
        // Fills right vertical in cell to make it looks rectangular.
        setCustomRenderer(globalHighlighterRenderer)

        // Render in gutter - separator line and filled selection.
        lineMarkerRenderer = NotebookLineMarkerRenderer()
      }

      repaint()
    }
    catch (e: Exception) {
      logger.error(e)
    }
  }

  private fun removeSelectionHighlighter() {
    selectionHighlighter?.let {
      editor.markupModel.removeHighlighter(it)
      selectionHighlighter = null
    }
  }

  override fun doLayout() {
    val borderInsets = border.getBorderInsets(this)
    components.forEach {
      it.setBounds(borderInsets.left,
                   borderInsets.top,
                   width - borderInsets.left - borderInsets.right,
                   height - borderInsets.bottom - borderInsets.top)
    }
  }

  override fun getMinimumSize(): Dimension = panel.minimumSize

  // We need this only for statistics.
  fun getTableDimensions(): Pair<Int, Int>? = outputController.getTableDimensions()

  /** Paints background of inlay component and selected inlay highlighted background. */
  override fun paintComponent(g: Graphics) {

    (g.create() as Graphics2D).use { g2d ->
      if (InlaysConfig.getInstance().outputHighlightMode == HighlightMode.SELECTED ||
          InlaysConfig.getInstance().outputHighlightMode == HighlightMode.ALL) {

        g2d.color = if (selected) getCodeCellBackground(editor.colorsScheme) else editor.backgroundColor

        g2d.fillRect(0, 0, width, InlayDimensions.topOffset + InlayDimensions.cornerRadius)
        g2d.fillRect(0, height - InlayDimensions.bottomOffset - InlayDimensions.cornerRadius, width,
                     InlayDimensions.bottomOffset + InlayDimensions.cornerRadius - editor.lineHeight / 2)

        if (selected) {
          g2d.fillRect(width - InlayDimensions.rightOffset, 0, InlayDimensions.rightOffset, height - editor.lineHeight / 2)
        }
      }

      if (InlaysConfig.getInstance().transparentOutput) {
        g2d.color = editor.backgroundColor
        g2d.fillRect(0, 0, width, height)
      }
      else {
        g2d.color = UIUtil.getLabelBackground()
        g2d.fillRoundRect(0, InlayDimensions.topOffset, width - InlayDimensions.rightOffset,
                          height - InlayDimensions.bottomOffset - InlayDimensions.topOffset,
                          InlayDimensions.cornerRadius, InlayDimensions.cornerRadius)
      }
    }
  }

  fun updateGutterComponentPosition() {
    val gutter = gutter ?: return

    gutterComponent?.apply {
      if (this@apply.x != 0 ||
          this@apply.y != this@NotebookInlayComponent.y ||
          this@apply.width != gutter.width ||
          this@apply.height != this@NotebookInlayComponent.height) {
        setBounds(0, this@NotebookInlayComponent.y, gutter.width, this@NotebookInlayComponent.height)
      }
      outputController.updateGutter(this)
    }
  }

  override fun assignInlay(inlay: Inlay<*>) {
    super.assignInlay(inlay)
    addOrUpdateGutter()
  }

  private fun addOrUpdateGutter() {
    if (gutterComponent != null) {
      return
    }

    gutterComponent = NotebookInlayComponentGutter(this)

    gutter = (editor.gutter as JComponent).apply {
      add(gutterComponent)
    }

    updateGutterComponentPosition()
  }

  override fun dispose() {

    if (gutterComponent != null) {
      gutter?.apply {
        remove(gutterComponent)
      }
      gutterComponent = null
      gutter = null
    }

    selectionHighlighter?.let {
      editor.markupModel.removeHighlighter(it)
      selectionHighlighter = null
    }

    super.dispose()
  }

  private fun collapse(saveHeight: Boolean = false) {

    if (collapsed) {
      return
    }

    if (saveHeight) {
      expandedHeight = size.height
    }

    outputController.detachTabs()
    statusController.detachStatusLabel()

    collapsed = true

    resizeController.uninstall()

    toolbarController.showCollapsedState(outputController.getCollapsedDescription())

    setPreferredHeight()

    cell.tableHide = true
  }

  private fun expand() {

    if (!collapsed) {
      return
    }

    toolbarController.removeCollapsedState()

    outputController.attachTabs()
    statusController.attachStatusLabel()

    if (expandedHeight != 0) {
      val borderInsets = border.getBorderInsets(this)
      setSize(width, max(minimumSize.height + borderInsets.top + borderInsets.bottom, expandedHeight))
      expandedHeight = 0
    }

    collapsed = false

    resizeController.install()
    repaint()

    cell.tableHide = false
  }

  fun onCollapseExpand() {
    if (!outputController.hasOutput()) {
      return
    }

    val oldOnChange = onChange
    onChange = null

    if (collapsed) {
      expand()
    }
    else {
      collapse(saveHeight = true)
    }

    onChange = oldOnChange
    onChange?.invoke()
  }

  override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
    if (this.x == x && this.y == y && this.width == width && this.height == height) {
      return
    }

    runReadAction {
      EditorScrollingPositionKeeper.perform(editor, true) {
        val heightChanged = this.height != height

        super.setBounds(x, y, max(minimumSize.width, width), max(minimumSize.height, height))

        updateGutterComponentPosition()

        if (heightChanged) {
          onHeightChanged?.invoke(userSized)
        }
      }
    }
  }

  private fun restoreSize(inlaySettings: JsonObject) {
    val sizeJson = inlaySettings.getAsJsonObject("size")
    if (sizeJson != null) {
      val currentOnChange = onChange
      onChange = null
      userSized = true
      onChange = currentOnChange
      setHeightLater(sizeJson["height"].asInt)
    }
  }

  fun onSettingsChanged() {
    outputController.onSettingsChanged()
    loadFromSettings()
  }

  fun loadFromSettings() {

    val settings = cell.asJsonTree()

    if (settings.isJsonNull) {
      return
    }

    if (cell.tableHide) {
      collapse()
    }
    else {
      expand()
    }

    val inlaySettings = cell.getMetadata("inlay")?.asJsonObjectOrNull()
    if (inlaySettings != null) {
      restoreSize(inlaySettings)
    }
  }

  /**
   * Returns settings object for this inlay component. We are saving height, collapsed state, current page and chart settings.
   */
  fun getSettings(): NotebookInlaySettings {
    return NotebookInlaySettings(if (userSized) if (collapsed || expandedHeight != 0) expandedHeight else height else null,
                                 collapsed = if (collapsed) true else null)
  }

  /** Event from notebook with cell execution percentage. Percentage is in range [0, 100]*/
  fun onProgress(percentage: Int) {

    // When we are aborting cell, status command comes first and then progress which is newer ended, so we will skip progress while in Aborted state.
    if (this.status == CellStatus.ABORT) {
      return
    }

    toolbarController.onProgress(percentage)
    if (percentage != 0) {
      toolbarController.setDescription(VisMessagesBundle.message("cell.status.runningPercents", percentage))
    }
  }

  private fun getPreferredHeight(): Int {
    return max(gutterComponent?.minimumHeight ?: 0,
               outputController.getPreferredHeight() + top.preferredSize.height + bottom.preferredSize.height) +
           InlayDimensions.topBorder + InlayDimensions.bottomBorder
  }

  private fun clearOnReady() {
    toolbarController.setDescription(CellStatus.READY.text)

    outputController.removeOutput()

    setPreferredHeight()
    collapsed = false
    resizeController.uninstall()

    collapsible = false
  }

  fun onClear() {

    if (status != CellStatus.PENDING) {
      toolbarController.hide()
    }

    outputController.removeOutput()

    if (userSized) {
      userSized = false
      onChange?.invoke()
    }

    setPreferredHeight()

    collapsed = false
    resizeController.uninstall()

    collapsible = false
  }

  private fun setPreferredHeight() {
    setSize(width, getPreferredHeight())
    revalidate()
    repaint()
  }

  fun setHeightLater(desiredHeight: Int) {
    runInEdt {
      if (editor.isDisposed) {
        return@runInEdt
      }
      if (collapsed) {
        // To save the height, if cell was collapsed.
        if (expandedHeight == 0) {
          expandedHeight = desiredHeight
        }
      }
      else {
        setSize(width, max(minimumSize.height, desiredHeight))
        revalidate()
        repaint()
      }
    }
  }

  /** Restore preferred size of inlay. Called from double click */
  fun restoreSize() {
    userSized = false
    outputController.adjustHeight()
    onChange?.invoke()
  }

  inner class NotebookLineMarkerRenderer : LineMarkerRenderer {
    override fun paint(editor: Editor, g: Graphics, r: Rectangle) {
      val gutterWidth = (editor.gutter as Component).width

      val y = y + height - editor.lineHeight / 2

      if (InlaysConfig.getInstance().drawSeparatorLine) {
        g.color = editor.colorsScheme.getColor(EditorColors.RIGHT_MARGIN_COLOR)
        g.drawLine(0, y, gutterWidth + 5, y)
      }

      // Filled vertical in gutter same color as cell.
      g.color = getCodeCellBackground(editor.colorsScheme)
      g.fillRect(gutterWidth - stripeOffset, r.y, stripeOffset, r.height - height)

      // Vertical line in gutter for selected cell.
      if (selected || !cell.isSynced) {
        g.color = if (cell.isSynced) getSelectedCellStripeColor(editor.colorsScheme) else getUnsyncCellStripeColor(editor.colorsScheme)
        g.fillRect(gutterWidth - stripeWidth - stripeOffset, r.y, stripeWidth, r.height)
      }
    }
  }
}