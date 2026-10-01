package com.intellij.python.huggingFace.cacheManager.ui.core

import com.intellij.ide.CopyProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.python.huggingFace.HuggingFaceProBundle
import com.intellij.ui.PopupHandler
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Component
import java.awt.event.MouseEvent
import java.util.Date
import javax.swing.DefaultListSelectionModel
import javax.swing.JComponent
import javax.swing.UIDefaults
import javax.swing.UIManager
import javax.swing.event.MouseInputAdapter
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

/**
 *  Table by latest guidelines
 *  https://jetbrains.github.io/ui/controls/table/
 */
class HfCacheMaterialTable(
  model: TableModel,
  private val updateCallback: () -> Unit
) : JBTable(model), Disposable, UiDataProvider, CopyProvider {
  private var rollOverRowIndex = -1
  private var initialized = false

  private var oneAndHalfRowHeight = false
    set(value) {
      if (value) {
        rowHeight = (tableHeader.defaultRenderer.getTableCellRendererComponent(this, "", false, false, 0,
                                                                               0).preferredSize.height * 1.5).toInt()
      }
      field = value
    }

  private val mouseListener = object : MouseInputAdapter() {
    override fun mouseExited(e: MouseEvent) {
      rollOverRowIndex = -1
      repaint()
    }

    override fun mouseMoved(e: MouseEvent) {
      val row = rowAtPoint(e.point)
      if (row != rollOverRowIndex) {
        rollOverRowIndex = row
        repaint()
      }
    }
  }

  init {
    selectionModel.selectionMode = DefaultListSelectionModel.SINGLE_SELECTION
    autoResizeMode = AUTO_RESIZE_NEXT_COLUMN
    rowSelectionAllowed = true
    cellSelectionEnabled = false
    columnSelectionAllowed = false
    autoCreateRowSorter = true
    setShowColumns(true)
    setShowGrid(false)
    emptyText.text = HuggingFaceProBundle.message("cache.table.empty.text")

    tableHeader.apply {
      defaultRenderer = HfCacheSimpleHeaderRenderer()
      reorderingAllowed = true
      resizingAllowed = true
    }

    assignColumnRenderers()
    setRowSorter()
    addMouseMotionListener(mouseListener)
    addMouseListener(mouseListener)
    PopupHandler.installPopupMenu(this, "HfCacheManagementContextMenu", "MaterialTable")
    initialized = true
  }

  override fun performCopy(dataContext: DataContext) { }
  override fun isCopyEnabled(dataContext: DataContext): Boolean = selectedRowCount > 0 && selectedColumnCount > 0
  override fun isCopyVisible(dataContext: DataContext): Boolean = true
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
  override fun isColumnSelected(column: Int): Boolean = false

  override fun uiDataSnapshot(sink: DataSink) {
    sink[PlatformDataKeys.COPY_PROVIDER] = this
    sink[BDI_TABLE] = this
  }

  override fun createDefaultRenderers() {
    defaultRenderersByColumnClass = UIDefaults(8, 0.75f)
    defaultRenderersByColumnClass[Any::class.java] = UIDefaults.LazyValue { HfCacheMaterialTableCellRenderer() }
    defaultRenderersByColumnClass[Number::class.java] = UIDefaults.LazyValue { HfCacheTableNumberRenderer() }
    defaultRenderersByColumnClass[Int::class.java] = UIDefaults.LazyValue { HfCacheTableNumberRenderer() }
    defaultRenderersByColumnClass[Date::class.java] = UIDefaults.LazyValue { HfCacheTableDateRenderer() }
    defaultRenderersByColumnClass[String::class.java] = UIDefaults.LazyValue { HfCacheTableStringRenderer() }
  }

  private fun assignColumnRenderers() = columnModel.apply {
    getColumn(0).cellRenderer = HfCacheTableStringRenderer()  // repo ID
    getColumn(1).cellRenderer = HfCacheTableStringRenderer()  // repo type
    getColumn(2).cellRenderer = HfCacheTableFileSizeRenderer()  // total size
    getColumn(3).cellRenderer = HfCacheTableDateRenderer()  // last accessed
    getColumn(4).cellRenderer = HfCacheTableDateRenderer()  // last modified
    getColumn(5).cellRenderer = HfCacheTableStringRenderer()  // path
  }

  private fun setRowSorter() {
    val sorter = TableRowSorter(model)
    sorter.setComparator(2, Comparator<Double> { o1, o2 -> o1.compareTo(o2)} )  // total size
    sorter.setComparator(3, Comparator<Date> { o1, o2 -> o1.compareTo(o2)} )  // last accessed
    sorter.setComparator(4, Comparator<Date> { o1, o2 -> o1.compareTo(o2)} )  // last modified
    rowSorter = sorter
  }

  override fun updateUI() {
    if (font != null) font = font.deriveFont(UIManager.getFont("Table.font").size.toFloat())
    // To restore extended row height after enabling to presentation mode.
    if (oneAndHalfRowHeight) oneAndHalfRowHeight = true
    super.updateUI()
    if (initialized) MaterialTableUtils.fitColumnsWidth(this)
    createDefaultRenderers()
  }

  /** We are preparing a renderer background for mouse hovered row. */
  override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
    val tableIsFocusOwner = isFocusOwner
    val c = super.prepareRenderer(renderer, row, column) as JComponent
    val (foregroundColor, backgroundColor) = selectCellColors(isRowSelected(row), isColumnSelected(column), tableIsFocusOwner, row)
    c.font = font
    c.foreground = foregroundColor
    c.background = backgroundColor
    return c
  }

  /**
   * @return (Foreground color, Background color)
   */
  private fun selectCellColors(
    isRowSelected: Boolean,
    isColumnSelected: Boolean,
    isFocusOwner: Boolean,
    row: Int,
  ): Pair<Color, Color> = when {
    isRowSelected -> {
      if (isColumnSelected) {
        if (isFocusOwner) Pair(getSelectionForeground(), getSelectionBackground())
        else Pair(getSelectionRowForeground(), getSelectionRowBackground())
      }
      else Pair(
        if (isFocusOwner) getSelectionRowForeground() else foreground,
        if (isFocusOwner) getSelectionRowBackground() else UIUtil.getTableSelectionBackground(false)
      )
    }
    row == rollOverRowIndex -> Pair(foreground, JBUI.CurrentTheme.Table.Hover.background(true))
    else -> Pair(foreground, background)
  }

  fun updateTableData(): Unit = updateCallback()
  private fun getSelectionRowBackground(): Color = UIUtil.getTableSelectionBackground(true)
  private fun getSelectionRowForeground(): Color = getSelectionForeground()
  override fun dispose() {}

  companion object {
    val BDI_TABLE: DataKey<HfCacheMaterialTable> = DataKey.create<HfCacheMaterialTable>("BDI_MATERIAL_TABLE_PARENT")
  }
}
