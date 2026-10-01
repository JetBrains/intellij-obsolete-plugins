package com.intellij.dbt.test

import com.intellij.dbt.console.commands.DbtCommand
import com.intellij.dbt.console.getDbtCommandLine
import com.intellij.dbt.run.DbtRunConfigurationOptions
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DbtCliUtilTest : BasePlatformTestCase() {
  fun testGetDbtCommandLine() {
    val cmd = getDbtCommandLine(DbtCommand.BUILD, myFixture.module, null)
    assertEquals("dbt build", cmd.commandLineString)
  }

  fun testOne() {
    val runConfigurationOptions = DbtRunConfigurationOptions()
    runConfigurationOptions.dbtCommand = DbtCommand.BUILD
    runConfigurationOptions.dbtAdditionalArguments.add("param1")

    val commandLine = runConfigurationOptions.getCommandLine(myFixture.module).commandLineString
    assertEquals("dbt build param1", commandLine)
  }
}