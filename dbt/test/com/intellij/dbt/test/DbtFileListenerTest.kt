package com.intellij.dbt.test

import com.intellij.dbt.DbtUtils
import com.intellij.testFramework.PlatformTestUtil

class DbtFileListenerTest : DbtTestCase() {
  fun testDetectDbtProject() {
    assertNull(DbtUtils.getDbtSettings(myFixture.module))
    myFixture.addFileToProject("dbt_project.yml", "")
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    assertNotNull(DbtUtils.getDbtSettings(myFixture.module))
  }

  fun testNoDbtInitializationUnderIgnoredDirectories() {
    assertNull(DbtUtils.getDbtSettings(myFixture.module))
    myFixture.addFileToProject("dbt_packages/dbt_project.yml", "")
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    assertNull(DbtUtils.getDbtSettings(myFixture.module))
  }

  override fun shouldPrepareDbtProject() = false
}