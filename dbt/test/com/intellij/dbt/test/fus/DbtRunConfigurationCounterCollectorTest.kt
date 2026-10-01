package com.intellij.dbt.test.fus

import com.intellij.dbt.console.commands.DbtCommand
import com.intellij.dbt.fus.DbtRunConfigurationCounterCollector.DBT_COMMANDS
import junit.framework.TestCase

class DbtRunConfigurationCounterCollectorTest : TestCase() {
  fun testAllDbtCommandRegisteredInFus() {
    DbtCommand.entries.forEach {
      if (!DBT_COMMANDS.contains(it.toString())) {
        fail("dbt command \"$it\" not registered in FUS")
      }
    }
  }
}