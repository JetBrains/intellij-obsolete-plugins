@file:Suppress("DialogTitleCapitalization")

package com.intellij.dbt.settings

import com.intellij.application.options.ModuleAwareProjectConfigurable
import com.intellij.dbt.DbtBundle
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

class DbtProjectConfigurable(project: Project) : ModuleAwareProjectConfigurable<DbtConfigurable>(project, DbtBundle.message("dbt.display.name"), "dbt") {
  override fun createModuleConfigurable(module: Module): DbtConfigurable {
    return DbtConfigurable(module)
  }
}