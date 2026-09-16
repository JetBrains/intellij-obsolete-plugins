package com.jetbrains.spark.submit.statistic

import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project
import com.jetbrains.spark.submit.run.SparkSubmitConfigurationType
import com.jetbrains.spark.submit.run.common.AbstractSparkJobRunConfiguration
import com.jetbrains.spark.submit.settings.RunConfigurationBlockType

class SparkSubmitStateCollector : ProjectUsagesCollector() {

  override fun getGroup() = GROUP

  override fun getMetrics(project: Project): Set<MetricEvent> {
    val configs = RunManagerEx.getInstanceEx(project)
      .allConfigurationsList.filterIsInstance<AbstractSparkJobRunConfiguration<*>>()

    return configs.map { config ->
      CONFIGURED_EVENT.metric(FACTORY_TYPE.with(config.factory?.type?.id),
                              FACTORY.with(config.factory?.id),
                              DRIVER_CLASS_PATHS_SIZE.with(config.driverClassPath.size),
                              DRIVER_LIBRARY_PATHS_SIZE.with(config.driverLibraryPath.size),
                              JARS_FILES_SIZE.with(config.jars.size),
                              PY_FILES_SIZE.with(config.pyFiles.size),
                              ARCHIVES_SIZE.with(config.archives.size),
                              VISIBLE_BLOCKS.with(config.visibleBlocks.mapNotNull { (key, value) -> key.name.takeIf { value } }))
    }.toSet()
  }

  private val GROUP = EventLogGroup("bigdatatools.spark.submit.configurations", 5)

  private val FACTORY_TYPE = EventFields.String("factory_type", SparkSubmitConfigurationType.ids)
  private val FACTORY = EventFields.String("factory", SparkSubmitConfigurationType.ids.flatMap {
    ConfigurationTypeUtil.findConfigurationType(it)?.configurationFactories?.toList() ?: emptyList()
  }.map(ConfigurationFactory::getId).distinct())
  private val DRIVER_CLASS_PATHS_SIZE = EventFields.RoundedInt("driver_class_paths_size")
  private val DRIVER_LIBRARY_PATHS_SIZE = EventFields.RoundedInt("driver_library_paths_size")
  private val JARS_FILES_SIZE = EventFields.RoundedInt("jars_files_size")
  private val PY_FILES_SIZE = EventFields.RoundedInt("py_files_size")
  private val ARCHIVES_SIZE = EventFields.RoundedInt("archives_size")
  private val VISIBLE_BLOCKS = EventFields.StringList("visible_blocks", RunConfigurationBlockType.entries.map { it.name })

  private val CONFIGURED_EVENT = GROUP.registerVarargEvent("connection.configured",
                                                           FACTORY_TYPE,
                                                           FACTORY,
                                                           DRIVER_CLASS_PATHS_SIZE,
                                                           DRIVER_LIBRARY_PATHS_SIZE,
                                                           JARS_FILES_SIZE,
                                                           PY_FILES_SIZE,
                                                           ARCHIVES_SIZE,
                                                           VISIBLE_BLOCKS)

}