package com.jetbrains.spark.monitoring.ui.table

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.table.MaterialTable
import org.jetbrains.annotations.Nls
import javax.swing.JLabel
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener

/** Updates given Label to "$text (${dataModel.size})" on dataModel updates. */
class LabelCountUpdater private constructor(private val table: MaterialTable,
                                            @Nls private val text: String,
                                            private val label: JLabel) : TableModelListener, Disposable {

  companion object {
    fun installOn(table: MaterialTable, @Nls text: String, label: JLabel) {
      val labelCountUpdater = LabelCountUpdater(table, text, label)
      Disposer.register(table, labelCountUpdater)
    }
  }

  init {
    table.model.addTableModelListener(this)
    modelChanged()
  }

  override fun tableChanged(e: TableModelEvent?) {
    modelChanged()
  }

  private fun modelChanged() {
    label.text = if (table.model.rowCount == 0) text else "$text (${table.rowCount})"
  }

  override fun dispose() {
    table.model.removeTableModelListener(this)
  }
}