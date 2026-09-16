package com.intellij.bigdatatools.zeppelin.statistics

import com.intellij.bigdatatools.notebooks.statistics.AllowedList
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.project.Project

object ZeppelinInterpreterUsageCollector : CounterUsagesCollector() {

  override fun getGroup() = GROUP

  private val GROUP = EventLogGroup("bigdatatools.zeppelin.interpreter", 7)

  private val driver_type = EventFields.String("driver_type", ZeppelinAllowedList.jdbcAllowedList + AllowedList.UNKNOWN)
  private val jdbcDriverUsedEvent = GROUP.registerEvent("jdbc.driver.used",
                                                        zeppelin_version,
                                                        driver_type)

  private val interpreters_count_total = EventFields.Int("interpreters_count_total")
  private val interpreters_count_custom_name = EventFields.Int("interpreters_count_custom_name")
  private val interpreters_count_custom_id = EventFields.Int("interpreters_count_custom_id")

  private val listGetEvent = GROUP.registerVarargEvent("list.get",
                                                       zeppelin_version,
                                                       interpreters_count_total,
                                                       interpreters_count_custom_name,
                                                       interpreters_count_custom_id)

  private val interpreter_type = EventFields.String("interpreter_type", ZeppelinAllowedList.interpreterGroupAllowedList)
  private val count_total = EventFields.Int("count_total")
  private val count_maven = EventFields.Int("count_maven")

  private val interpreterDependenciesUsedEvent = GROUP.registerVarargEvent("interpreter.dependencies.used",
                                                                           zeppelin_version,
                                                                           interpreter_type,
                                                                           count_total,
                                                                           count_maven)

  fun collectInterpreterSettings(project: Project?,
                                 context: UsageContext,
                                 interpreterSettings: List<InterpreterSettings>) {
    sendJdbcUsage(interpreterSettings, project, context)

    interpreterSettings.forEach { interpreterSetting ->
      sendInterpreterUsage(project, context, interpreterSetting)
      val group = ZeppelinAllowedList.interpreterGroup(interpreterSetting.group)
      val dependencies = interpreterSetting.dependencies
      interpreterDependenciesUsedEvent.log(project,
                                           zeppelin_version.with(context.zeppelinVersion.toStatisticsString()),
                                           interpreter_type.with(group),
                                           count_total.with(dependencies.size),
                                           count_maven.with(dependencies.count { it.isMaven() }))
    }

    listGetEvent.log(project,
                     zeppelin_version.with(context.zeppelinVersion.toStatisticsString()),
                     interpreters_count_total.with(interpreterSettings.size),
                     interpreters_count_custom_name.with(
                       interpreterSettings.count { ZeppelinAllowedList.interpreterGroup(it.name) == AllowedList.UNKNOWN }),
                     interpreters_count_custom_id.with(
                       interpreterSettings.count { ZeppelinAllowedList.interpreterGroup(it.group) == AllowedList.UNKNOWN }))
  }

  private val interpreterUsedEvent = GROUP.registerEvent("interpreter.used",
                                                         zeppelin_version,
                                                         interpreter_type,
                                                         EventFields.Boolean("name_is_default"))

  private fun sendInterpreterUsage(project: Project?,
                                   context: UsageContext,
                                   interpreterSettings: InterpreterSettings) {
    val group = ZeppelinAllowedList.interpreterGroup(interpreterSettings.group)
    interpreterUsedEvent.log(project,
                             context.zeppelinVersion.toStatisticsString(),
                             group,
                             interpreterSettings.name == group)
  }

  private fun sendJdbcUsage(interpreterSettings: List<InterpreterSettings>,
                            project: Project?,
                            context: UsageContext) {
    val jdbcInterpreters = interpreterSettings.filter { it.group == "jdbc" }
    jdbcInterpreters.forEach {
      sendJdbTypeUsage(project, context, it)
    }
  }

  private fun sendJdbTypeUsage(project: Project?,
                               context: UsageContext,
                               jdbcInterpreter: InterpreterSettings) {
    val driverType = jdbcInterpreter.properties["default.driver"]?.value as? String ?: return
    val name = ZeppelinAllowedList.jdbcDriver(driverType)
    jdbcDriverUsedEvent.log(project, context.zeppelinVersion.toStatisticsString(), name)
  }
}
