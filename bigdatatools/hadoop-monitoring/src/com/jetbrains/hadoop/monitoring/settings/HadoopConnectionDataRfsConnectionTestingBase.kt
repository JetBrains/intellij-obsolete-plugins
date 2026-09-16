package com.jetbrains.hadoop.monitoring.settings

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.settings.RfsConnectionTestingBase
import com.jetbrains.bigdatatools.common.settings.defaultui.SettingsPanelCustomizerEx

class HadoopConnectionDataRfsConnectionTestingBase(
  project: Project,
  settingsCustomizer: SettingsPanelCustomizerEx<HadoopConnectionData>?
) : RfsConnectionTestingBase<HadoopConnectionData>(project, settingsCustomizer)