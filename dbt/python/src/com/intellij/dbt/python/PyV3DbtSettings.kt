package com.intellij.dbt.python

import com.intellij.dbt.python.PyDbtUtil.Companion.defaultSettingsDirectory
import com.intellij.dbt.settings.DbtNewProjectSettings
import com.intellij.dbt.settings.NEW_PROFILE_OPTION_NAME
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.python.errorProcessing.PyResult
import com.jetbrains.python.newProjectWizard.PyV3ProjectTypeSpecificSettings

class PyV3DbtSettings(
  var profile: Profile = NEW_PROFILE_OPTION_NAME,
) : PyV3ProjectTypeSpecificSettings {
  override suspend fun generateProject(module: Module, baseDir: VirtualFile, sdk: Sdk): PyResult<Unit> {
    val settings: DbtNewProjectSettings = object : DbtNewProjectSettings {
      override fun getDbtProfile(): String = ""

      override fun getDbtSettingsDirectory(): String = defaultSettingsDirectory
    }
    PyDbtUtil.configureNewDbtProject(module.project, baseDir, settings, sdk, module)

    return PyResult.success(Unit)
  }
}