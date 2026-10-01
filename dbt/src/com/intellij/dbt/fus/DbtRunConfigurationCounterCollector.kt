package com.intellij.dbt.fus

import com.intellij.dbt.console.commands.DbtCommand
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector

internal object DbtRunConfigurationCounterCollector  : CounterUsagesCollector() {
  override fun getGroup(): EventLogGroup = GROUP

  private val GROUP: EventLogGroup = EventLogGroup("dbt.run.configuration.invocation", 1)

  val DBT_COMMANDS = listOf("build", "clone", "compile", "debug", "deps", "docs", "list", "retry", "run", "run-operation", "seed", "show",
                                    "snapshot", "source", "test")

  private val RUN_CONFIGURATION_INVOKED_EVENT = GROUP.registerEvent("run.configuration.invoked", EventFields.String("name", DBT_COMMANDS))

  fun logRunEvent(command: DbtCommand) {
    val commandName = command.toString()
    if (commandName in DBT_COMMANDS) {
      RUN_CONFIGURATION_INVOKED_EVENT.log(commandName)
    }
  }
}