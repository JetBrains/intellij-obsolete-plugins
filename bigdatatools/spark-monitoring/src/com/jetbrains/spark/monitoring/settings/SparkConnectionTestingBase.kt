package com.jetbrains.spark.monitoring.settings

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.settings.RfsConnectionTestingBase
import com.jetbrains.bigdatatools.common.settings.defaultui.SettingsPanelCustomizerEx

class SparkConnectionTestingBase(
  project: Project,
  settingsCustomizer: SettingsPanelCustomizerEx<SparkConnectionData>?
) : RfsConnectionTestingBase<SparkConnectionData>(project, settingsCustomizer)