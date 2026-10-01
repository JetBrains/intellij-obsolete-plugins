package com.intellij.dataWrangler.impl

import com.intellij.dataWrangler.DataWranglerSession
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.impl.ui.DWTableDataViewer
import com.intellij.dataWrangler.impl.ui.DataWranglerUiSession
import com.intellij.database.datagrid.DataGrid
import com.intellij.database.datagrid.DataGridListener
import com.intellij.database.datagrid.GridRequestSource
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class DataWranglerUiSessionImpl<C : DataWranglerContext>(
  override val backendSession: DataWranglerSession<C>,
  override val tableViewer: DWTableDataViewer,
) : DataWranglerUiSession<C> {
  override val dataChanges: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  init {
    tableViewer.getGrid().addDataGridListener(object : DataGridListener {
      override fun onContentChanged(dataGrid: DataGrid?, place: GridRequestSource.RequestPlace?) {
        dataChanges.tryEmit(Unit)
      }
    }, this)
  }

  override fun dispose() {
  }
}