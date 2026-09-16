package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import com.jetbrains.hadoop.monitoring.util.HadoopLocalizedField

data class HadoopConfigurationProperty(
  val name: String,
  val value: String,
  val final: Boolean,
  val source: String
) : RemoteInfo {
  companion object {
    val renderableColumns: List<HadoopLocalizedField<HadoopConfigurationProperty>> by lazy {
      listOf(
        HadoopLocalizedField(HadoopConfigurationProperty::name, "data.HadoopConfigurationProperty.name"),
        HadoopLocalizedField(HadoopConfigurationProperty::value, "data.HadoopConfigurationProperty.value"),
        HadoopLocalizedField(HadoopConfigurationProperty::final, "data.HadoopConfigurationProperty.final"),
        HadoopLocalizedField(HadoopConfigurationProperty::source, "data.HadoopConfigurationProperty.source")
      )
    }
  }
}