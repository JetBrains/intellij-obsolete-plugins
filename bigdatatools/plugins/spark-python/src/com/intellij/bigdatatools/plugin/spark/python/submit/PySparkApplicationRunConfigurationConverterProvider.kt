package com.intellij.bigdatatools.plugin.spark.python.submit

import com.intellij.conversion.ConversionContext
import com.intellij.conversion.ConversionProcessor
import com.intellij.conversion.ConverterProvider
import com.intellij.conversion.ProjectConverter
import com.intellij.conversion.RunManagerSettings
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.jetbrains.spark.submit.run.SparkSubmitConfigurationType
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import org.jdom.Element

internal class PySparkApplicationRunConfigurationConverterProvider : ConverterProvider() {
  override fun getConversionDescription(): String {
    return SparkMessagesBundle.message("spark.converter.build.run.configuration.description")
  }

  override fun createConverter(context: ConversionContext): ProjectConverter {
    return object : ProjectConverter() {
      override fun createRunConfigurationsConverter(): ConversionProcessor<RunManagerSettings> {
        return object : ConversionProcessor<RunManagerSettings>() {
          fun isMigratableSparkConfiguration(element: Element): Boolean =
            element.getAttributeValue("type").equals(SparkSubmitConfigurationType.SPARK_ID, ignoreCase = true) &&
            element.getAttributeValue("default") != "true"

          fun isJavaIde(): Boolean {
            return PluginManagerCore.getPlugin(PluginId.findId("com.intellij.java")) != null
          }
          override fun isConversionNeeded(settings: RunManagerSettings): Boolean {
            if (isJavaIde()) return false
            return settings.runConfigurations.any { isMigratableSparkConfiguration(it) }
          }
          override fun process(settings: RunManagerSettings) {
            for (element in settings.runConfigurations) {
              if (isMigratableSparkConfiguration(element)) {
                element.setAttribute("type", SparkSubmitConfigurationType.PYSPARK_ID)
              }
            }
          }
        }
      }
    }
  }

}