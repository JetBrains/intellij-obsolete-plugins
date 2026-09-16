package com.intellij.bigdatatools.databricks.settings

import com.intellij.bigdatatools.databricks.rfs.DatabricksConnectionData
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.settings.RfsConnectionTestingBase

class DatabricksConnectionTesting(
  project: Project,
  settingsCustomizer: DatabricksSettingsCustomizer
) : RfsConnectionTestingBase<DatabricksConnectionData>(project, settingsCustomizer)