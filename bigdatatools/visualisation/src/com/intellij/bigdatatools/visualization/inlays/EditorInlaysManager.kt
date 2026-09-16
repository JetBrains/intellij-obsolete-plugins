package com.intellij.bigdatatools.visualization.inlays

import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellAdded
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellChanged
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellRemoved
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookChangeListener
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookEvent
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookSchema
import com.intellij.bigdatatools.notebooks.core.impl.editor.NotebookEditorUtils
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultMessage
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.OutputCode
import com.intellij.bigdatatools.visualization.inlays.settings.SettingsChangeListener
import com.intellij.bigdatatools.visualization.inlays.style.InlaysConfig
import com.intellij.bigdatatools.visualization.statistics.VisualizationChangesCollector
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FoldingListener
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.ExperimentalUI
import com.intellij.ui.scale.JBUIScale
import com.jetbrains.bigdatatools.common.util.invokeLater
import org.jetbrains.concurrency.AsyncPromise
import java.awt.Component
import java.awt.event.AdjustmentEvent
import java.awt.event.AdjustmentListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.concurrent.atomic.AtomicLong
import javax.swing.BorderFactory
import javax.swing.border.Border

class CellResultMessageDelayed(type: CellResultType = CellResultType.NULL,
                               data: String,
                               val promise: AsyncPromise<String>) : CellResultMessage(type, data)

/**
 * Manages inlays.
 * ToDo should be split into InlaysManager with all basics and NotebookInlaysManager with all specific.
 */
class EditorInlaysManager private constructor(private val project: Project,
                                              private val editor: EditorImpl,
                                              notebookVirtualFile: NotebookVirtualFile) : NotebookChangeListener {

  companion object {
    private val logger = Logger.getInstance(this::class.java)
    val gutterBorder: Border
      get() = if (ExperimentalUI.isNewUI()) BorderFactory.createEmptyBorder(0, 0, 0, 4) else BorderFactory.createEmptyBorder()

    private fun getNotebookVirtualFile(editor: Editor): NotebookVirtualFile? {
      return (FileDocumentManager.getInstance().getFile(editor.document) ?: return null) as? NotebookVirtualFile
    }

    fun installOn(editor: Editor): EditorInlaysManager? {
      val project = editor.project ?: return null
      if (editor !is EditorImpl) return null
      val notebookVirtualFile = getNotebookVirtualFile(editor) ?: return null

      return EditorInlaysManager(project, editor, notebookVirtualFile)
    }
  }

  /** List of all inlays in manager. Always sorted by position. */
  val inlays = HashMap<NotebookCell, NotebookInlayComponent>()
  private val titles = HashMap<NotebookCell, NotebookInlayTitle>()

  private val editorLastScrollTimestamp = AtomicLong(0L)

  private var performCaretPositionChangeOnBatchEnd = false

  private val performSelectionUpdateOnBatchEnd = mutableListOf<NotebookInlayComponent>()

  private val disposable = Disposer.newDisposable()

  private val resizeListener = object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) = updateInlayComponentsWidth()
  }

  private val gutterResizeListener = object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) = updateGutterComponentsPosition()
  }

  private val cellToolbarController: NotebookCellToolbarController

  private val inlaysMouseController: EditorInlaysMouseListener

  private val settingsListener = SettingsChangeListener {
    inlays.forEach { it.value.statusController.updateStatus() }
  }

  private val headerInlay = InlayComponent()

  private var updateInlaysYPosScheduled = false

  init {
    val startTimestamp = System.currentTimeMillis()

    val gutterComponentEx = (editor as EditorEx).gutterComponentEx
    if (InlaysConfig.getInstance().transparentGutter) {
      gutterComponentEx.isPaintBackground = false
    }

    gutterComponentEx.border = gutterBorder

    val notebook = notebookVirtualFile.notebook
    addTopSpacing()
    notebook.addNotebookChangeListener(this)
    /** On editor resize all inlays got width of editor. */
    editor.component.addComponentListener(resizeListener)
    gutterComponentEx.addComponentListener(gutterResizeListener)
    addCaretListener()

    inlaysMouseController = EditorInlaysMouseListener(this, editor)
    Disposer.register(editor.disposable, inlaysMouseController)

    cellToolbarController = NotebookCellToolbarController()
    Disposer.register(editor.disposable, cellToolbarController)

    inlaysMouseController.addListener(cellToolbarController)

    editor.foldingModel.addListener(object : FoldingListener {
      override fun onFoldProcessingEnd() = updateInlaysYPos()
    }, disposable)
    editor.settings.isRightMarginShown = false
    UISettings.getInstance().showEditorToolTip = false
    restoreOutputAndSettings(notebookVirtualFile)
    updateInlayComponentsWidth()
    onCaretPositionChanged()

    editor.scrollPane.verticalScrollBar.addAdjustmentListener(MyScrollBarListener(editorLastScrollTimestamp))
    VisualizationChangesCollector.logVisualizationLoadEvent(notebook, notebookVirtualFile,
                                                            System.currentTimeMillis() - startTimestamp)

    InlaysConfig.getInstance().addListener(settingsListener)
  }

  //region NotebookChangeListener
  override fun onEvent(notebookEvent: NotebookEvent) {

    when (notebookEvent) {
      is CellAdded -> {
        onCellAdded(notebookEvent.cell)
        performCaretPositionChangeOnBatchEnd = true
      }

      is CellRemoved -> {
        onCellRemoved(notebookEvent.cell)
        performCaretPositionChangeOnBatchEnd = true
      }

      is CellChanged -> onCellChanged(notebookEvent)
    }

    if (notebookEvent.isLastInBatch) {
      if (performCaretPositionChangeOnBatchEnd) {
        onCaretPositionChanged()
        performCaretPositionChangeOnBatchEnd = false
      }

      if (performSelectionUpdateOnBatchEnd.isNotEmpty()) {
        performSelectionUpdateOnBatchEnd.forEach {
          if (it.selected) {
            it.updateSelectionHighlighter()
          }
        }
        performSelectionUpdateOnBatchEnd.clear()
      }
    }
  }
  //endregion NotebookChangeListener

  private fun onCellChanged(notebookEvent: CellChanged) {

    val changedFields = notebookEvent.changedFields
    val cell = notebookEvent.cell

    if (changedFields.contains(NotebookSchema.results)) {
      onOutputUpdated(cell)
    }

    if (changedFields.contains(NotebookSchema.title)) {
      onTitleChanged(cell)
    }

    if (changedFields.contains(NotebookSchema.cellStatus)) {
      onStatusUpdated(cell)
    }

    if (changedFields.contains(NotebookSchema.cellSource)) {
      onSourceChanged(cell)
    }

    if (changedFields.contains(NotebookSchema.syncStatus)) {
      titles[cell]?.synced = cell.isSynced
      inlays[cell]?.synced = cell.isSynced
    }

    if (changedFields.contains(NotebookSchema.dateUpdated) ||
        changedFields.contains(NotebookSchema.dateCreated) ||
        changedFields.contains(NotebookSchema.dateStarted) ||
        changedFields.contains(NotebookSchema.dateFinished) ||
        changedFields.contains(NotebookSchema.user)) {
      onDatesChanged(cell)
    }

    if (changedFields.contains(NotebookSchema.cellConfig)) {
      onConfigChanged(cell)
      onTitleChanged(cell) // In config we have title visibility flag.
    }
  }

  private fun onSourceChanged(cell: NotebookCell) {
    val inlayComponent = inlays[cell] ?: return

    //ToDo The only thing we should handle here - new line was added or removed and -> we need to change offset or position
    val textRange = NotebookEditorUtils.getCellRangeInEditor(editor, cell)
    val textLinesRange = TextRange(editor.offsetToVisualLine(textRange.startOffset), editor.offsetToVisualLine(textRange.endOffset))
    if (textLinesRange == inlayComponent.textLinesRange) {
      return
    }

    updateInlayOffset(inlayComponent)
    inlayComponent.updateSelectionHighlighter()
    updateNextCellTitleOffset(cell)

    inlayComponent.textLinesRange = textLinesRange

    // This is for all inlays, and we should avoid this!
    updateInlaysYPos()

    // Hack.
    // We are getting event "caret position change" before event "cell changed" while moving lines with Ctrl+Shift+Arrow and that's why
    // getting wrong cell under caret.
    // Now, when getting "source changed" for unselected cell we are updating all selections.
    if (!inlayComponent.selected || performCaretPositionChangeOnBatchEnd) {
      performCaretPositionChangeOnBatchEnd = true
      performSelectionUpdateOnBatchEnd.add(inlayComponent)
    }
  }

  private fun onTitleChanged(cell: NotebookCell) {
    val title = titles[cell]

    if (cell.titleVisible && title == null) {
      addInlayTitle(cell)
      invokeLater {
        titles[cell]?.let {
          IdeFocusManager.getInstance(project).requestFocus(it.title, true)
        }
      }
    }
    else if (!cell.titleVisible && title != null) {
      removeInlayTitle(cell)
    }

    titles[cell]?.title?.text = cell.title
  }

  private fun onConfigChanged(cell: NotebookCell) {
    inlays[cell]?.onSettingsChanged()
  }

  private fun onDatesChanged(cell: NotebookCell) {
    inlays[cell]?.statusController?.updateStatus()
  }

  private fun restoreOutputAndSettings(notebookVirtualFile: NotebookVirtualFile) {

    notebookVirtualFile.notebook.cells.forEach { cell ->

      addInlayTitle(cell)
      val inlayComponent = addInlayComponent(cell)

      val cellOutput = cell.output
      if (cellOutput != null && cellOutput.msg.isNotEmpty()) {
        inlayComponent.outputController.onOutput(notebookVirtualFile.notebook, cellOutput.code, cellOutput.msg, true)
      }
      else {
        // ToDo DataBricks special output
        // inlayComponent.outputController.processPossibleMarkdownCell()

        if (!inlayComponent.hasOutput()) {
          val status = cell.status
          if (status != CellStatus.FINISHED && status != CellStatus.READY) {
            onStatusUpdated(cell)
          }
        }
      }

      restoreSettings(cell)

      setupInlayComponent(inlayComponent)

      inlayComponent.statusController.updateStatus()
    }
  }

  fun dispose() {
    InlaysConfig.getInstance().removeListener(settingsListener)
    Disposer.dispose(disposable)
    getNotebookVirtualFile(editor)?.notebook?.removeNotebookChangeListener(this)
    editor.component.removeComponentListener(resizeListener)
    (editor.gutter as? Component)?.removeComponentListener(gutterResizeListener)
    inlays.forEach { Disposer.dispose(it.value) }
    inlays.clear()
  }

  /** Add caret listener for editor to draw highlighted background for psiCell under caret. */
  private fun addCaretListener() {
    editor.caretModel.addCaretListener(object : CaretListener {
      override fun caretPositionChanged(e: CaretEvent) {
        if (editor.caretModel.primaryCaret != e.caret) return
        onCaretPositionChanged()
      }
    }, editor.disposable)
  }

  private fun onCaretPositionChanged() {

    if (editor.isDisposed || inlays.isEmpty()) {
      return
    }

    if (editor.document.isInBulkUpdate) return

    val caretOffset = editor.logicalPositionToOffset(editor.caretModel.logicalPosition)

    if (caretOffset == 0) return

    val cellUnderCaret = inlays.values.find { it.cellTextRange.contains(caretOffset) }
                         ?: inlays.values.maxByOrNull { it.cell.textRange.startOffset }

    if (cellUnderCaret == null) {
      inlays.forEach { it.value.selected = false }
      titles.forEach { it.value.selected = false }
    }
    else {
      if (!cellUnderCaret.selected) {
        inlays.forEach { it.value.selected = false }
        titles.forEach { it.value.selected = false }
        cellUnderCaret.selected = true
        titles[cellUnderCaret.cell]?.selected = true

        cellToolbarController.cellSelected(cellUnderCaret)
      }
    }
  }

  private fun setupInlayComponent(inlayComponent: NotebookInlayComponent) {

    inlayComponent.onChange = {
      val inlaySettings = inlayComponent.getSettings()
      NotebookInlaySettingsZeppelinAdapter.save(inlaySettings, inlayComponent.cell)

      VisualizationChangesCollector.logVisualizationChangeEvent(inlayComponent, inlaySettings)
    }

    inlayComponent.onHeightChanged = { userSized ->
      updateInlaysYPos()
      if (userSized) {
        NotebookInlaySettingsZeppelinAdapter.save(inlayComponent.getSettings(), inlayComponent.cell)
      }
    }
  }

  private fun updatePreviousCellInlayOffset(cell: NotebookCell) {
    cell.note?.let { note ->
      val cellIndex = cell.indexInNote
      if (cellIndex > 0) {
        inlays[note.cells[cell.indexInNote - 1]]?.let { updateInlayOffset(it) }
      }
    }
  }

  private fun updateNextCellTitleOffset(cell: NotebookCell) {
    cell.note?.let { note ->
      val cellIndex = cell.indexInNote
      if (note.cells.size > cellIndex + 1) {
        titles[note.cells[cellIndex + 1]]?.let { updateTitleOffset(it) }
      }
    }
  }

  private fun onCellAdded(cell: NotebookCell) {

    // Hack. When we are adding cell in notebook, previous cell changes their range and we need to update it.
    //       Also, when adding cell, the next cell title get invalid position and we need to fix it.
    updatePreviousCellInlayOffset(cell)
    updateNextCellTitleOffset(cell)

    addInlayTitle(cell)
    val inlayComponent = addInlayComponent(cell)
    setupInlayComponent(inlayComponent)

    onStatusUpdated(cell)
    onOutputUpdated(cell)
    restoreSettings(cell)
  }

  private fun onCellRemoved(cell: NotebookCell) {
    inlays[cell]?.let {

      cellToolbarController.cellRemoved(it)

      it.parent?.remove(it)
      Disposer.dispose(it)
      inlays.remove(it.cell)
    }

    titles[cell]?.let {
      it.parent?.remove(it)
      Disposer.dispose(it)
      titles.remove(it.cell)
    }
  }

  private fun updateGutterComponentsPosition() {
    inlays.forEach { it.value.updateGutterComponentPosition() }
    titles.forEach { it.value.updateGutterComponentPosition() }
  }

  /** Aligns all editor inlays to fill full width of editor. */
  private fun updateInlayComponentsWidth() {
    val inlayWidth = editor.scrollingModel.visibleArea.width
    headerInlay.setSize(inlayWidth, headerInlay.height)
    if (inlayWidth > 0) {
      inlays.forEach { it.value.setSize(inlayWidth, it.value.height) }
    }

    val titleWidth = inlayWidth - editor.scrollPane.verticalScrollBar.width
    if (titleWidth > 0) {
      titles.forEach { it.value.setSize(titleWidth, it.value.height) }
    }
  }

  fun updateInlaysYPos() {
    if (editor.isDisposed || updateInlaysYPosScheduled) {
      return
    }

    updateInlaysYPosScheduled = true
    invokeLater {
      if (editor.isDisposed) {
        updateInlaysYPosScheduled = false
        return@invokeLater
      }

      titles.forEach {
        it.value.setLocation(it.value.x, editor.offsetToXY(it.key.textRange.startOffset).y - it.value.height)
      }

      inlays.forEach {
        it.value.setLocation(it.value.x, getInlayY(it.value))
      }
      updateInlaysYPosScheduled = false
    }
  }

  private fun updateTitleOffset(title: NotebookInlayTitle) {
    if (title.inlay?.offset != title.cell.textRange.startOffset) {
      title.inlay?.let { Disposer.dispose(it) }
      val inlay = editor.inlayModel.addBlockElement(title.cell.textRange.startOffset, false, true, 1, title)
      title.assignInlay(inlay)
    }
  }

  /** It could be that user started to type below inlay. In this case we will detect new position and perform inlay repositioning. */
  private fun updateInlayOffset(inlayComponent: NotebookInlayComponent) {
    if (inlayComponent.inlay?.offset != getInlayOffset(inlayComponent)) {
      inlayComponent.inlay?.let { Disposer.dispose(it) }
      inlayComponent.assignInlay(addBlockElement(inlayComponent))
    }
  }

  /** Adds inlay of 40px height to give us some free space to display toolbars over the note cells. */
  private fun addTopSpacing() {
    InlayDimensions.init(editor)

    // On editor creation it has 0 width
    var editorWideWidth = editor.component.width - (editor.gutter as Component).width
    if (editorWideWidth <= 0) {
      editorWideWidth = InlayDimensions.width
    }

    headerInlay.setBounds(0, 0, editorWideWidth, JBUIScale.scale(40))

    editor.contentComponent.add(headerInlay)

    val inlay = editor.inlayModel.addBlockElement(0, false, true, 0, headerInlay)
    headerInlay.assignInlay(inlay)
  }

  private fun restoreSettings(cell: NotebookCell) {
    val component = inlays[cell] ?: return
    val oldOnChange = component.onChange
    component.onChange = null
    component.loadFromSettings()
    component.onChange = oldOnChange
  }

  private fun onStatusUpdated(cell: NotebookCell) = cell.status.let { inlays[cell]?.status = it }

  private fun onOutputUpdated(cell: NotebookCell) {
    val inlayComponent = inlays[cell] ?: return
    val out = cell.output
    if (out == null) {
      inlayComponent.onClear()
    }
    else {
      inlayComponent.outputController.onOutput(cell.note, out.code, out.msg, true)
    }
  }

  private fun getInlayY(inlayComponent: NotebookInlayComponent): Int {
    return editor.offsetToXY(getInlayOffset(inlayComponent)).y + editor.lineHeight
  }

  /** inlay offset == cell end offset. */
  private fun getInlayOffset(inlayComponent: NotebookInlayComponent): Int {
    return NotebookEditorUtils.getCellRangeInEditor(editor, inlayComponent.cell).endOffset
  }

  private fun addBlockElement(inlayComponent: NotebookInlayComponent): Inlay<NotebookInlayComponent> {
    return editor.inlayModel.addBlockElement(getInlayOffset(inlayComponent), true, false, 0, inlayComponent)
  }

  private class MyScrollBarListener(private val editorLastScrollTimestamp: AtomicLong) : AdjustmentListener {
    override fun adjustmentValueChanged(e: AdjustmentEvent) {
      if (!e.valueIsAdjusting) editorLastScrollTimestamp.set(System.currentTimeMillis())
    }
  }

  private fun removeInlayTitle(cell: NotebookCell) {
    val found = titles[cell] ?: return
    found.parent?.remove(found)
    Disposer.dispose(found)
    titles.remove(cell)
  }

  private fun addInlayTitle(cell: NotebookCell) {

    if (!cell.titleVisible) {
      return
    }

    val existingInlay = titles[cell]
    if (existingInlay != null) {
      logger.warn("Cell already added.")
      return
    }

    InlayDimensions.init(editor)

    val inlayComponent = inlays[cell]

    val inlayTitle = NotebookInlayTitle(editor, cell).apply {
      selected = inlayComponent?.selected ?: false
      synced = inlayComponent?.synced ?: true
    }

    Disposer.register(editor.disposable, inlayTitle)
    val titleWidth = editor.scrollingModel.visibleArea.width - editor.scrollPane.verticalScrollBar.width
    inlayTitle.setBounds(0, editor.offsetToXY(cell.textRange.startOffset).y - InlayDimensions.lineHeight,
                         if (titleWidth <= 0) InlayDimensions.width else titleWidth, InlayDimensions.lineHeight)
    editor.contentComponent.add(inlayTitle)

    val inlay = editor.inlayModel.addBlockElement(cell.textRange.startOffset, false, true, 1, inlayTitle)
    inlayTitle.assignInlay(inlay)

    titles[cell] = inlayTitle

    return
  }

  private fun addInlayComponent(cell: NotebookCell): NotebookInlayComponent {

    val existingInlay = inlays[cell]
    if (existingInlay != null) {
      logger.warn("Cell already added.")
      return existingInlay
    }

    InlayDimensions.init(editor)

    val inlayComponent = NotebookInlayComponent(editor, cell, editorLastScrollTimestamp)

    EditorScrollingPositionKeeper.perform(editor, true) {
      Disposer.register(editor.disposable, inlayComponent)
      val inlayWidth = editor.scrollingModel.visibleArea.width
      inlayComponent.setBounds(0, getInlayY(inlayComponent) + editor.lineHeight,
                               if (inlayWidth <= 0) InlayDimensions.width else inlayWidth, InlayDimensions.smallHeight)

      editor.contentComponent.add(inlayComponent)
      val inlay = addBlockElement(inlayComponent)

      inlayComponent.assignInlay(inlay)
    }

    inlays[cell] = inlayComponent

    return inlayComponent
  }

  fun onProgress(cell: NotebookCell, percentage: Int) {
    invokeAndWaitIfNeeded {
      inlays[cell]?.onProgress(percentage)
    }
  }

  fun onOutput(cell: NotebookCell, data: String, update: Boolean, type: CellResultType) {
    if(cell.getMetadata("ZTOOLS_DEBUG_CELL_ID")?.asString != null) {
      return
    }

    invokeAndWaitIfNeeded {
      inlays[cell]?.outputController?.onOutput(cell.note, OutputCode.INCOMPLETE, listOf(CellResultMessage(type, data)), update)
    }
  }
}