package com.intellij.dbt.test

import com.intellij.jinja.Jinja2CodeInsightSettings

class DbtTypingTest : DbtTestCase() {
  fun testValueTagCompletionWhenSettingsOn() {
    runTestWithAutoInsertTagValue(true) {
      myFixture.configureByFiles("models/model.sql")
      myFixture.type("{{")
      myFixture.checkResult("{{  }}")
    }
  }

  fun testValueTagCompletionWhenSettingsOff() {
    runTestWithAutoInsertTagValue(false) {
      myFixture.configureByFiles("models/model.sql")
      myFixture.type("{{")
      myFixture.checkResult("{{}")
    }
  }

  fun testTagCompletionWhenSettingsOn() {
    runTestWithAutoInsertTagValue(true) {
      myFixture.configureByFiles("models/model.sql")
      myFixture.type("{%")
      myFixture.checkResult("{%  %}")
    }
  }

  fun testTagCompletionWhenSettingsOff() {
    runTestWithAutoInsertTagValue(false) {
      myFixture.configureByFiles("models/model.sql")
      myFixture.type("{%")
      myFixture.checkResult("{%}")
    }
  }

  fun testInsertClosingDoubleQuote() {
    myFixture.configureByFiles("models/quotes.sql")
    myFixture.type("\"")
    myFixture.checkResult("{{ ref(\"\") }}")
  }

  fun testInsertClosingSingleQuote() {
    myFixture.configureByFiles("models/quotes.sql")
    myFixture.type("'")
    myFixture.checkResult("{{ ref('') }}")
  }

  fun testNoInsertClosingDoubleQuote() {
    myFixture.configureByFiles("models/unclosed_double_quote.sql")
    myFixture.type("\"")
    myFixture.checkResult("{{ ref(\"\") }}")
  }

  fun testNoInsertClosingSingleQuote() {
    myFixture.configureByFiles("models/unclosed_single_quote.sql")
    myFixture.type("'")
    myFixture.checkResult("{{ ref('') }}")
  }

  override fun getBasePath(): String = "${super.getBasePath()}/typing"

  private fun runTestWithAutoInsertTagValue(value: Boolean, test: Runnable) {
    val oldValue = Jinja2CodeInsightSettings.getInstance().JINJA2_AUTOINSERT_TAG_CLOSE
    try {
      Jinja2CodeInsightSettings.getInstance().JINJA2_AUTOINSERT_TAG_CLOSE = value
      test.run()
    }
    finally {
      Jinja2CodeInsightSettings.getInstance().JINJA2_AUTOINSERT_TAG_CLOSE = oldValue
    }
  }
}