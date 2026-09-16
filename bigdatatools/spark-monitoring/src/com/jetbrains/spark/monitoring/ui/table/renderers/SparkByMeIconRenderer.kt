package com.jetbrains.spark.monitoring.ui.table.renderers

import com.intellij.icons.AllIcons
import com.jetbrains.bigdatatools.common.table.renderers.AbstractIconRenderer
import com.jetbrains.spark.monitoring.util.SMMessagesBundle
import java.awt.Component
import javax.swing.JTable

class SparkByMeIconRenderer : AbstractIconRenderer(emptyMap(), null) {
  override fun getTableCellRendererComponent(table: JTable?,
                                             value: Any?,
                                             isSelected: Boolean,
                                             hasFocus: Boolean,
                                             row: Int,
                                             column: Int): Component {
    this.text = ""
    this.toolTipText = ""
    if (value == true) {
      this.icon = AllIcons.General.User
      this.toolTipText = SMMessagesBundle.message("app.run.by.me")
    }
    else
      this.icon = null
    return this
  }
}