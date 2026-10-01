package com.intellij.dbt.test

import com.intellij.dbt.detection.DbtService
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

abstract class DbtTestCase : BasePlatformTestCase() {
  override fun setUp() {
    super.setUp()

    if (shouldPrepareDbtProject()) {
      myFixture.copyFileToProject("dbt_project.yml")
    }

    detectDbtSupport()
  }

  override fun getBasePath(): String {
    return "/plugins/dbt/testData"
  }

  protected open fun shouldPrepareDbtProject() = true

  protected fun detectDbtSupport(): Unit = runBlocking {
    val module = myFixture.module as ModuleBridge
    val dbtService = DbtService.getInstance(project)
    dbtService.coroutineScope.launch {
      dbtService.processModule(module)
    }
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
  }
}