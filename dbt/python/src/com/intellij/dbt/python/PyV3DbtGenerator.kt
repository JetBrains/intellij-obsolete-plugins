package com.intellij.dbt.python

import com.intellij.dbt.DbtBundle
import com.intellij.dbt.DbtIcons
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.python.newProjectWizard.PyV3ProjectBaseGenerator

class PyV3DbtGenerator : PyV3ProjectBaseGenerator<PyV3DbtSettings>(PyV3DbtSettings(), PyV3DbtUI) {
  override fun getName() = DbtBundle.message("dbt.display.name")

  override fun getLogo() = DbtIcons.Dbt

  override val projectTypeForStatistics: @NlsSafe String = "com.intellij.dbt.python.PyDbtProjectGenerator"
}