package com.intellij.dataWrangler.impl.view

import com.intellij.dataWrangler.impl.DataWranglerBundle
import com.intellij.openapi.util.NlsContexts

internal object DWProperties {
  const val GAP_WIDTH = 10

  // Probably will have more to come
  data class PanelInfo(@NlsContexts.Label val panelTitle: String, val toolbarPlaceName: String)

  //val TRANSFORMATION_PANEL_INFO = PanelInfo(
  //  DataWranglerBundle.message("label.transformations.panel.title"),
  //  "DWTransformationPanel"
  //)
  val SUMMARY_PANEL_INFO = PanelInfo(
    DataWranglerBundle.message("label.summary.panel.title"),
    "DWSummaryPanel"
  )
  val STEPS_PANEL_INFO = PanelInfo(
    DataWranglerBundle.message("label.steps.panel.title"),
    "DWHistoryPanel"
  )
}