package com.jetbrains.spark.monitoring.ui.table.renderers

import com.jetbrains.bigdatatools.common.table.renderers.MaterialTableCellRenderer
import com.jetbrains.bigdatatools.common.table.renderers.SplitStringRenderer
import com.jetbrains.bigdatatools.common.util.SizeUtils
import com.jetbrains.bigdatatools.common.util.TimeUtils
import java.awt.Component
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.swing.JTable

class SummaryColumnRenderer : MaterialTableCellRenderer() {

  companion object {
    var format = DecimalFormat("0.#")
  }

  override fun getTableCellRendererComponent(table: JTable, value: Any?, isSelected: Boolean,
                                             hasFocus: Boolean, row: Int, column: Int): Component {

    val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

    if (column != 0) {
      //https://github.com/LucaCanali/Miscellaneous/blob/master/Spark_Notes/Spark_TaskMetrics.md
      when (table.getValueAt(row, 0)) {
        "executorCpuTime",
        "executorDeserializeCpuTime"
        -> setValue(TimeUtils.intervalAsString(TimeUnit.NANOSECONDS.toMillis(value as Long)))

        "executorRunTime",
        "executorDeserializeTime",
        "jvmGcTime",
        "resultSerializationTime",
        "shuffleReadFetchWaitTime",
        "shuffleWriteWriteTime"
        -> setValue(TimeUtils.intervalAsString(value as Long))

        "resultSize",
        "memoryBytesSpilled",
        "diskBytesSpilled",
        "inputBytesRead",
        "outputBytesWritten",
        "shuffleReadRemoteBytesRead",
        "shuffleReadLocalBytesRead",
        "shuffleWriteBytesWritten",
        "peakExecutionMemory"
        -> setValue(SizeUtils.toString(value as Long))

        else -> {
          try {
            setValue(format.format(value))
          }
          catch (e: Exception) {
            setValue(value.toString())
          }
        }
      }
    }
    else {
      setValue(SplitStringRenderer.camelCaseToReadable(value))
    }

    return component
  }
}
