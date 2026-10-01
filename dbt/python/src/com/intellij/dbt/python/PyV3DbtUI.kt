package com.intellij.dbt.python

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.toNullableProperty
import com.jetbrains.python.PyBundle
import com.jetbrains.python.newProjectWizard.PyV3ProjectTypeSpecificUI
import com.jetbrains.python.newProjectWizard.projectPath.ProjectPathProvider

object PyV3DbtUI : PyV3ProjectTypeSpecificUI<PyV3DbtSettings> {
  override val advancedSettings: (Panel.(PyV3DbtSettings, ProjectPathProvider) -> Unit)? = { settings, _ ->
    val model = PyV3DbtViewModel()
    row {
      comboBox(model.getProfilesModel()).validationOnApply {
        return@validationOnApply if (model.isFullyLoaded) null else ValidationInfo(PyBundle.message("python.add.sdk.panel.wait"))
      }.bindItem(settings::profile.toNullableProperty())
    }
  }
}