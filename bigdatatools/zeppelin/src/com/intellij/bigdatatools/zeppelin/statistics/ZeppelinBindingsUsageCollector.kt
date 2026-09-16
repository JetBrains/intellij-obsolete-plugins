package com.intellij.bigdatatools.zeppelin.statistics

import com.intellij.bigdatatools.zeppelin.models.interpreter.Interpreter
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector

object ZeppelinBindingsUsageCollector : CounterUsagesCollector() {

  override fun getGroup() = GROUP

  private val interpreter_count_total = EventFields.Int("count_total")
  private val interpreter_count_selected = EventFields.Int("count_selected")

  private val interpreter_group = EventFields.String("group", ZeppelinAllowedList.interpreterGroupAllowedList)
  private val interpreter_name_is_default = EventFields.Boolean("name_is_default")

  private val GROUP = EventLogGroup("bigdatatools.zeppelin.bindings", 6)
  private val bindingsUsageEvent = GROUP.registerEvent("updated",
                                                       zeppelin_version,
                                                       interpreter_count_total,
                                                       interpreter_count_selected)

  private val defaultInterpreterUsageEvent = GROUP.registerEvent("default.used",
                                                                 zeppelin_version,
                                                                 interpreter_group,
                                                                 interpreter_name_is_default)

  fun bindingsUpdate(context: UsageContext, bindings: List<Interpreter>) {

    bindingsUsageEvent.log(context.zeppelinVersion.toStatisticsString(), bindings.size, bindings.count { it.selected })

    val defaultBinding = bindings.firstOrNull() ?: return
    val defaultInterpreterBinding = ZeppelinAllowedList.bindingInterpreterId(defaultBinding)
    val isDefaultName = defaultInterpreterBinding == defaultBinding.id
    defaultInterpreterUsageEvent.log(context.zeppelinVersion.toStatisticsString(), defaultInterpreterBinding, isDefaultName)
  }
}