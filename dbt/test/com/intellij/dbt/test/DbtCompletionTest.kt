package com.intellij.dbt.test

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.dbt.DbtBundle

class DbtCompletionTest: DbtTestCase() {
  fun testDbtTagCompletion() {
    myFixture.configureByFiles("models/model_d.sql")
    val completion = myFixture.completeBasic()
    assertNull(completion)
    assertEquals("{% debug() %}", myFixture.file.text)
  }

  fun testDbtTagCompletionDoubleBraces() {
    myFixture.configureByFiles("models/model_c.sql")
    val completion = myFixture.completeBasic()
    assertNull(completion)
    assertEquals("{{ debug() }}", myFixture.file.text)
  }

  fun testDbtVariableCompletion() {
    myFixture.configureByFiles("models/var.sql")
    val completion = myFixture.completeBasic()
    assertTrue(completion.any { it.lookupString == "ref" })
  }

  fun testModelNameCompletionInRef() {
    myFixture.configureByFiles("models/model_b.sql", "models/model_a.sql")
    val completion = myFixture.completeBasic()
    val lookup = completion.firstOrNull { it.lookupString == "model_a" }
    assertNotNull(lookup)

    val presentation = LookupElementPresentation()
    lookup!!.renderElement(presentation)
    assertEquals(DbtBundle.message("dbt.completion.model.suffix"), presentation.typeText)
  }

  fun testSeedFileNameCompletionInRef() {
    myFixture.configureByFiles("models/model_a.sql", "seeds/raw_seed.csv")
    val completion = myFixture.completeBasic()
    val lookup = completion.firstOrNull { it.lookupString == "raw_seed" }
    assertNotNull(lookup)

    val presentation = LookupElementPresentation()
    lookup!!.renderElement(presentation)
    assertEquals(DbtBundle.message("dbt.completion.seed.suffix"), presentation.typeText)
  }

  override fun getBasePath(): String = "${super.getBasePath()}/completion"
}