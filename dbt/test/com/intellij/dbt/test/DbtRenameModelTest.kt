package com.intellij.dbt.test

import com.intellij.refactoring.rename.RenameProcessor

class DbtRenameModelTest : DbtTestCase() {
  fun testRenameModel() {
    myFixture.configureByFiles("models/model_with_reference.sql", "models/model_b.sql")
    val element = myFixture.elementAtCaret

    RenameProcessor(project, element, "model_bb.sql", true, true).run()

    assertTrue(myFixture.file.text.contains("ref(\"model_bb\")"))
  }

  override fun getBasePath(): String = "${super.getBasePath()}/resolve"
}