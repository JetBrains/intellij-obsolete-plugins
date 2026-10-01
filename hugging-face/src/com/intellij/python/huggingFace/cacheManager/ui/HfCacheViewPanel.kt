package com.intellij.python.huggingFace.cacheManager.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.python.huggingFace.HuggingFaceProBundle
import com.intellij.python.huggingFace.cacheManager.service.HfCacheEntryData
import com.intellij.python.huggingFace.cacheManager.service.HfCacheScanner
import com.intellij.python.huggingFace.cacheManager.ui.core.HfCacheMaterialTable
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.util.Date

class HfCacheViewPanel(val project: Project) : SimpleToolWindowPanel(false, true), Disposable {
  private val table: HfCacheMaterialTable
  private val tableModel: ListTableModel<HfCacheEntryData>
  private val tablePanel: JBScrollPane
  private val scanner = HfCacheScanner.instance

  init {
    val columns = createColumns()
    tableModel = ListTableModel<HfCacheEntryData>(*columns.toTypedArray())
    table = HfCacheMaterialTable(tableModel, this::updateTableContent)

    val actionToolbar = createToolbar()
    toolbar = actionToolbar.component

    tablePanel = JBScrollPane(table)
    setContent(tablePanel)
    Disposer.register(this, table)
    updateData()
  }

  override fun dispose() {}

  private fun createToolbar(): ActionToolbar {
    val toolbarFactory = HfCacheToolbarFactory(
      project,
      this::getSelectedItems
    ) { updateData() }
    return toolbarFactory.createToolbar().apply {
      targetComponent = this@HfCacheViewPanel
    }
  }

  private fun updateData() {
    scanner.updateCachedData { updateTableContent() }
    if (content != tablePanel) setContent(tablePanel)
  }

  private fun updateTableContent() {
    val customData = scanner.getCachedData()
    while (tableModel.rowCount != 0) { tableModel.removeRow(tableModel.rowCount - 1) }
    tableModel.addRows(customData)
  }

  private fun getSelectedItems(): List<HfCacheEntryData> {
    val selectedRow = table.selectedRows
    return selectedRow.map { tableModel.getItem(it) }
  }

  private fun createColumns(): List<ColumnInfo<HfCacheEntryData, *>> = listOf(
      object : ColumnInfo<HfCacheEntryData, String>(HuggingFaceProBundle.message("cache.table.column.repoId")) {
        override fun valueOf(item: HfCacheEntryData?): String? = item?.repoId
      },
      object : ColumnInfo<HfCacheEntryData, String>(HuggingFaceProBundle.message("cache.table.column.repo.type")) {
        override fun valueOf(item: HfCacheEntryData?): String? = item?.repoType?.printName
      },
      object : ColumnInfo<HfCacheEntryData, Number>(HuggingFaceProBundle.message("cache.table.column.size.on.disk")) {
        override fun valueOf(item: HfCacheEntryData?): Double? = item?.sizeOnDisk
      },
      object : ColumnInfo<HfCacheEntryData, Date>(HuggingFaceProBundle.message("cache.table.column.last.accessed")) {
        override fun valueOf(item: HfCacheEntryData?): Date? = item?.lastAccessed
      },
      object : ColumnInfo<HfCacheEntryData, Date>(HuggingFaceProBundle.message("cache.table.column.last.modified")) {
        override fun valueOf(item: HfCacheEntryData?): Date? = item?.lastModified
      },
      object : ColumnInfo<HfCacheEntryData, String>(HuggingFaceProBundle.message("cache.table.column.local.path")) {
        override fun valueOf(item: HfCacheEntryData?): String = item?.path.toString()
      }
  )
}
