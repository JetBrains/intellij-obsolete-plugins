package com.intellij.dataWrangler.impl.ui

import com.intellij.dataWrangler.DataWranglerSession
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.openapi.Disposable
import kotlinx.coroutines.flow.Flow

interface DataWranglerUiSession<C : DataWranglerContext>: Disposable {
  val backendSession: DataWranglerSession<C>
  val tableViewer: DWTableDataViewer

  val dataChanges: Flow<Unit>
}