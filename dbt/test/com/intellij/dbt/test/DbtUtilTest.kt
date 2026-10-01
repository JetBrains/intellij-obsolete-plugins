package com.intellij.dbt.test

import com.intellij.dbt.DbtUtils
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import java.io.File

class DbtUtilTest : DbtTestCase() {
  fun testIsDbtDirectoryForDbtProject() {
    val dbtProjectDirectory = myFixture.project.getBaseDirectories().first()

    assertTrue(DbtUtils.containsDbtProjectFile(dbtProjectDirectory))
  }

  fun testIsDbtDirectoryForNotDbtProject() {
    val dbtProjectFile = myFixture.configureByFile("formatter/model.sql").parent

    assertFalse(DbtUtils.containsDbtProjectFile(dbtProjectFile!!.virtualFile))
  }

  fun testGuessDbtExecutable() {
    val dbtExecutableVirtualFile = myFixture.configureByFile("venv/bin/dbt")
    val dbtExecutablePath = dbtExecutableVirtualFile.virtualFile.path
    val dbtExecutableFile = File(dbtExecutablePath)
    dbtExecutableFile.setExecutable(true)

    val guessedDbtExecutableFile = DbtUtils.guessDbtExecutable(myFixture.module)
    assertEquals(dbtExecutablePath, guessedDbtExecutableFile)
  }
}