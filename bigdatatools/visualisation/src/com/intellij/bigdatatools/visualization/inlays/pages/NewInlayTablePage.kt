package com.intellij.bigdatatools.visualization.inlays.pages

import com.intellij.bigdatatools.visualization.inlays.NotebookInlayComponent
import com.intellij.bigdatatools.visualization.inlays.components.TabChangeListener
import com.intellij.bigdatatools.visualization.inlays.settings.SettingsChangeListener
import com.intellij.bigdatatools.visualization.table.settings.TablePaginationSettings
import com.intellij.bigdatatools.visualization.table.settings.TableSettings
import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.charts.dataframe.DataFrame
import com.intellij.charts.dataframe.DataFrameKeys.DATA_FRAME_DATA_KEY
import com.intellij.database.csv.CsvSettingsService
import com.intellij.database.datagrid.DataGrid
import com.intellij.database.datagrid.GridHelper
import com.intellij.database.datagrid.GridPagingModel
import com.intellij.database.datagrid.GridRequestSource
import com.intellij.database.datagrid.GridUtil
import com.intellij.database.datagrid.MultiPageModel.PageModelListener
import com.intellij.database.run.actions.SHOW_PAGINATION
import com.intellij.database.run.actions.enablePagination
import com.intellij.database.run.ui.DataAccessType
import com.intellij.database.run.ui.DataGridRequestPlace
import com.intellij.database.run.ui.TableResultPanel
import com.intellij.database.settings.DataGridSettings
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.JBUI
import com.jetbrains.bigdatatools.common.table.BdiGridHelper
import com.jetbrains.bigdatatools.common.table.BdiTableProvider
import com.jetbrains.bigdatatools.common.table.models.DataFrameHookUp
import javax.swing.JComponent

internal class InlayTablePage(dataFrame: DataFrame, inlay: NotebookInlayComponent) : InlayPage {
  var isDefault: Boolean = true
  override var indexInResults = -1
  private val grid: DataGrid
  override val component: JComponent
    get() = grid.panel.component
  override val title: String = VisMessagesBundle.message("page.title.table")
  override val contentType: InlayPageContentType = InlayPageContentType.TABLE

  /** Callback is called when page settings changed. */
  private val listeners = mutableListOf<SettingsChangeListener>()
  private var listenersDisabled = false

  init {
    val project = inlay.editor.project!!
    val hookUp = DataFrameHookUp(project, dataFrame)
    val helper = BdiGridHelper(true)
    hookUp.pageModel.pageSize = GridPagingModel.UNLIMITED_PAGE_SIZE
    grid = object : TableResultPanel(project, hookUp, GridUtil.getGridPopupActions(), { grid, appearance ->
      appearance.setResultViewSetShowHorizontalLines(false)
      appearance.setTransparentColumnHeaderBackground(true)
      GridHelper.set(grid, helper)
      BdiTableProvider.configure(grid, appearance)
      GridUtil.putSettings(grid, CsvSettingsService.getDatabaseSettings() as? DataGridSettings)
    }) {
      override fun uiDataSnapshot(sink: DataSink) {
        super.uiDataSnapshot(sink)
        sink[PlatformCoreDataKeys.FILE_EDITOR] = GridUtil.getOrCreateEditorWrapper(this, project) { "" }
        sink[DATA_FRAME_DATA_KEY] = dataFrame
      }
    }
    grid.putUserData(GridUtil.IN_EDITOR_RESULTS, true)
    Disposer.register(inlay, grid)
    GridUtil.addVerticalGridHeaderComponent(grid, "Console.EditorTableResult.Bdi.Vertical.Group")
    grid.panel.centerComponent.border = JBUI.Borders.customLine(JBUI.CurrentTheme.Editor.BORDER_COLOR, 1)

    hookUp.loader.loadFirstPage(GridRequestSource(GridRequestSource.GridRequestPlace { grid }))
    hookUp.pageModel.addPageModelListener(object : PageModelListener {
      override fun pageSizeChanged() {
        fireChangeEvent()
      }

      override fun pageStartChanged() {
        fireChangeEvent()
      }
    })
  }

  fun getTableDimensions(): Pair<Int, Int> {
    return Pair(grid.visibleColumnCount, grid.visibleRowsCount)
  }

  fun tabChanged(name: String) {
    tabChangeListeners.forEach { it.tabChanged(name) }
  }

  fun applySettings(settings: TableSettings) {
    disableListeners {
      setPagination(settings)
    }
  }

  private fun setPagination(settings: TableSettings) {
    val source = GridRequestSource(DataGridRequestPlace(grid))
    val hookUp = grid.dataHookup

    enablePagination(grid, settings.showPagination)

    if (settings.showPagination) {
      val pageSize = settings.pagination.pageSize
      hookUp.pageModel.pageSize = pageSize
      val pageIdx = settings.pagination.currentPage - 1
      hookUp.loader.load(source, pageIdx * pageSize)
    }
    else {
      hookUp.pageModel.pageSize = GridPagingModel.UNLIMITED_PAGE_SIZE
      hookUp.loader.load(source, 0)
    }
  }

  private fun disableListeners(func: () -> Unit) {
    listenersDisabled = true
    try {
      func()
    }
    finally {
      listenersDisabled = false
    }
  }

  fun addChangeListener(listener: SettingsChangeListener) {
    listeners += listener
  }

  private fun fireChangeEvent() {
    if (!listenersDisabled) {
      listeners.forEach { it.stateChanged() }
    }
  }

  private val tabChangeListeners = mutableListOf<TabChangeListener>()

  fun addTabChangeListener(listener: TabChangeListener) {
    tabChangeListeners += listener
  }

  fun getSettings(): TableSettings {
    val showPagination = SHOW_PAGINATION[grid] == true
    val paginationSettings = if (showPagination) {
      val pageModel = grid.dataHookup.pageModel
      val pageSize = pageModel.pageSize
      TablePaginationSettings((pageModel.pageStart - 1 + pageSize) / pageSize, pageSize)
    }
    else TablePaginationSettings(1, 250)
    return TableSettings(emptyList(), paginationSettings, false, showPagination, false)
  }

  override fun getCollapsedDescription(): String {
    val model = grid.getDataModel(DataAccessType.DATABASE_DATA)
    if (model.columnCount == 0) return "Empty table"

    val columnList = model.columns.joinToString { it.name }
    return "Table, rows: ${model.rowCount}, columns: $columnList"
  }
}