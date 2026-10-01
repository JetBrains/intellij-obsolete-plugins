package com.intellij.dbt.test.run

import com.intellij.dbt.console.commands.DbtCommand
import com.intellij.dbt.run.DbtRunConfiguration
import com.intellij.dbt.run.producer.DbtRunRunConfigurationProducer
import com.intellij.dbt.test.DbtTestCase
import com.intellij.execution.actions.ConfigurationContext

class DbtRunConfigurationProducerTest : DbtTestCase() {
  fun testUseExistingBuildConfigurationWhenCreateFromContext() {
    myFixture.configureByFiles("models/model.sql")
    val elementAtCaret = myFixture.file.findElementAt(0)
    val configs = ConfigurationContext(elementAtCaret!!).configurationsFromContext
    assertNotNull(configs)
    val dbtRunConfiguration = configs!!.map { it.configuration }.filterIsInstance<DbtRunConfiguration>().firstOrNull()
    assertNotNull(dbtRunConfiguration)
    assertEquals(DbtCommand.RUN, dbtRunConfiguration!!.getDbtOption().getCommand())
    dbtRunConfiguration.setDbtCommand(DbtCommand.BUILD)

    assertTrue(DbtRunRunConfigurationProducer().isConfigurationFromContext(dbtRunConfiguration,  ConfigurationContext(elementAtCaret)))
  }

  fun testDbtPackagesDirectory() {
    myFixture.configureByFiles("dbt_packages/model.sql")

    val elementAtCaret = myFixture.file.findElementAt(0)
    val configs = ConfigurationContext(elementAtCaret!!).configurationsFromContext
    assertNotNull(configs)
    val dbtRunConfiguration = configs!!.map { it.configuration }.filterIsInstance<DbtRunConfiguration>().firstOrNull()
    assertNull(dbtRunConfiguration)
  }

  fun testTestsDirectory() {
    myFixture.configureByFiles("tests/model_test.sql")

    val elementAtCaret = myFixture.file.findElementAt(0)
    val configs = ConfigurationContext(elementAtCaret!!).configurationsFromContext
    assertNotNull(configs)
    val dbtRunConfiguration = configs!!.map { it.configuration }.filterIsInstance<DbtRunConfiguration>().firstOrNull()
    assertNotNull(dbtRunConfiguration)
    assertEquals(DbtCommand.TEST, dbtRunConfiguration!!.getDbtOption().getCommand())
  }

  override fun getBasePath(): String = "${super.getBasePath()}/sample_project"
}