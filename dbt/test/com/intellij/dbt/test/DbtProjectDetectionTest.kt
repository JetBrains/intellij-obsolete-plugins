package com.intellij.dbt.test

import com.intellij.dbt.DbtUtils.Companion.findDbtDirectory
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DbtProjectDetectionTest : BasePlatformTestCase() {
  fun testDbtProjectFileDetection() {
    val dbtFolderExpected = myFixture.configureByFiles("level_1/dbt_project.yml", "level_1/level_2/dbt_project.yml").first()
    val dbt_directory = findDbtDirectory(myFixture.module)
    assertNotNull(dbt_directory)
    assertEquals(dbtFolderExpected.getVirtualFile().parent.path, dbt_directory!!.path)
  }

  override fun getBasePath(): String {
    return "/plugins/dbt/testData/detection"
  }
}