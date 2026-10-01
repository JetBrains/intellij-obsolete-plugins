package com.intellij.dataWrangler.impl.view.summary

import com.intellij.dataWrangler.impl.view.DWMainPanel
import com.intellij.dataWrangler.impl.view.DWProperties
import com.intellij.database.datagrid.DataGrid
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout

internal class SummaryPanel(grid: DataGrid) : JBPanel<SummaryPanel>(BorderLayout()) {
  private val PROPERTIES = DWProperties.SUMMARY_PANEL_INFO
  private val panel = DWMainPanel.createPanel(PROPERTIES.panelTitle, PROPERTIES.toolbarPlaceName, DefaultActionGroup())

  init {
    add(panel, BorderLayout.NORTH)
  }
}