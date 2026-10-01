package com.intellij.dataWrangler.impl.fus

import com.intellij.dataWrangler.operations.DataWranglerCommand
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector

enum class DataWranglerInputType {
  LOCAL_FILE,
  PYTHON_VARIABLE
}

object DataWranglerProviderCollector : CounterUsagesCollector() {
  override fun getGroup(): EventLogGroup = GROUP

  private val GROUP = EventLogGroup("data.wrangler", 2)

  private val INPUT_DATA_TYPE = EventFields.Enum<DataWranglerInputType>("dw_input_type")

  private val COMMAND_PROVIDER = EventFields.Class("executed_command", "Command DW provider")

  private val HISTORY_SIZE = EventFields.Int("history_size")

  private val HISTORY = EventFields.ClassList("executed_commands")

  private val DATA_WRANGLER_OPENED = GROUP.registerVarargEvent("data.wrangler.opened",
                                                               INPUT_DATA_TYPE)

  private val DATA_WRANGLER_COMMAND_EXECUTED = GROUP.registerVarargEvent("data.wrangler.command.executed",
                                                                         COMMAND_PROVIDER)

  private val DATA_WRANGLER_CODE_EXPORTED = GROUP.registerVarargEvent("data.wrangler.code.exported",
                                                                      HISTORY,
                                                                      HISTORY_SIZE)

  private val DATA_WRANGLER_CSV_EXPORT = GROUP.registerVarargEvent(
    "data.wrangler.file.export",
    HISTORY,
    HISTORY_SIZE,
  )

  fun logDWOpened(type: DataWranglerInputType) {
    DATA_WRANGLER_OPENED.log(INPUT_DATA_TYPE.with(type))
  }

  fun logDWOperationExecuted(
    commandProvider: DataWranglerCommand<*>,
  ) {
    DATA_WRANGLER_COMMAND_EXECUTED.log(
      COMMAND_PROVIDER.with(commandProvider.javaClass),
    )
  }

  fun logDWCodeExport(
    history: List<DataWranglerCommand<*>>,
  ) {
    DATA_WRANGLER_CODE_EXPORTED.log(
      HISTORY.with(history.map { it.javaClass }),
      HISTORY_SIZE.with(history.size)
    )
  }

  fun logDWCSVExport(
    history: List<DataWranglerCommand<*>>,
  ) {
    DATA_WRANGLER_CSV_EXPORT.log(
      HISTORY.with(history.map { it.javaClass }),
      HISTORY_SIZE.with(history.size)
    )
  }
}