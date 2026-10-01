package com.intellij.dbt.test

import com.intellij.dbt.DbtModuleEntity
import com.intellij.dbt.dbtSettings
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge

class DbtWorkspaceModelTest : DbtTestCase() {
  fun testDbtSettingsEntityForDbtProject() {
    myFixture.configureByFile("dbt_project.yml")
    detectDbtSupport()

    val dbtSettings = getDbtSettings()
    assertNotNull(dbtSettings)
  }

  fun testDbtSettingsEntityForDbtProjectInSubDirectory() {
    myFixture.addFileToProject("subdirectory/dbt_project.yml", "")
    detectDbtSupport()

    val dbtSettings = getDbtSettings()
    assertNotNull(dbtSettings)
  }

  fun testDbtSettingsEntityForNotDbtProject() {
    myFixture.configureByFile("formatter/model.sql")

    detectDbtSupport()

    val dbtSettings = getDbtSettings()
    assertNull(dbtSettings)
  }

  override fun shouldPrepareDbtProject() = false

  private fun getDbtSettings(): DbtModuleEntity? {
    val module = myFixture.module as ModuleBridge

    val newModuleEntity = module.findModuleEntity(module.entityStorage.current)!!
    val dbtSettings = newModuleEntity.dbtSettings
    return dbtSettings
  }
}