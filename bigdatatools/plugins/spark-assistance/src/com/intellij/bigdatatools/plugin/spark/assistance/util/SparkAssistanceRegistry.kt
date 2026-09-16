package com.intellij.bigdatatools.plugin.spark.assistance.util

import com.intellij.bigdatatools.plugin.spark.assistance.util.SparkAssistanceRegistry.ENABlE_DATAFRAME_ASSISTANCE
import com.intellij.codeInsight.hints.InlayHintsSettings
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.lang.Language
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryValue
import com.intellij.openapi.util.registry.RegistryValueListener
import com.intellij.profile.codeInspection.InspectionProfileManager

object SparkAssistanceRegistry {

  val SETTINGS_KEY_PYTHON = SettingsKey<NoSettings>("python.dataframe.inlay.hints")
  val SETTINGS_KEY_SCALA = SettingsKey<NoSettings>("scala.dataframe.inlay.hints")

  val ENABlE_DATAFRAME_ASSISTANCE: RegistryValue
    get() = Registry.get("spark.dataframe.code.assistance")
}

class SparkAssistanceRegistryListener : RegistryValueListener {
  override fun afterValueChanged(value: RegistryValue) {
    if (value.key == ENABlE_DATAFRAME_ASSISTANCE.key) {
      updateFlagSate(value.asBoolean())
    }
  }
  private fun updateFlagSate(state: Boolean) {
    Language.findLanguageByID("Python")?.let { pythonLanguage ->
      InlayHintsSettings.instance().changeHintTypeStatus(SparkAssistanceRegistry.SETTINGS_KEY_PYTHON, pythonLanguage, state)
    }
    Language.findLanguageByID("Scala")?.let { scalaLanguage ->
      InlayHintsSettings.instance().changeHintTypeStatus(SparkAssistanceRegistry.SETTINGS_KEY_SCALA, scalaLanguage, state)
    }
    for (profile in InspectionProfileManager.getInstance().profiles) {
      if (!profile.isProfileLocked) {
        profile.modifyProfile {
          it.setToolEnabled("PySparkDataFrameColumnInspection", state)
        }
      }
    }
  }
}