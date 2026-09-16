package com.jetbrains.spark.submit.util

import com.intellij.bigdatatools.coreUi.serializer.BdtJson
import com.intellij.bigdatatools.coreUi.ui.components.ConnectionProperty
import com.intellij.util.ResourceUtil
import com.squareup.moshi.Json

object SparkPropertiesUtils {
  fun loadSparkProperties(): List<ConnectionProperty> {
    val inputStream = ResourceUtil.getResourceAsStream(this::class.java.classLoader, "configs", "spark-params.json")
    val text = ResourceUtil.loadText(inputStream)
    val parsed = BdtJson.fromJsonArray(text, SparkProperty::class.java)
    return convertSparkToCommon(parsed)
  }

  private fun convertSparkToCommon(sparkProperties: List<SparkProperty>) = sparkProperties.filter { it.propertyName != null }
    .map {
      ConnectionProperty(propertyName = it.propertyName!!, default = it.default, meaning = it.meaning,
                         rightSideInfo = it.sinceVersion.removePrefix("vv").removeSuffix("++").let { "v$it+" })
    }

  data class SparkProperty(
    @Json(name = "Property Name")
    val propertyName: String?,
    @Json(name = "Default")
    val default: String = "",
    @Json(name = "Meaning")
    val meaning: String = "",
    @Json(name = "Since Version")
    val sinceVersion: String = ""
  )
}