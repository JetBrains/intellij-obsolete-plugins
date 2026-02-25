/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.groovy.grails.i18n

import com.intellij.groovy.grails.i18n.GrailsI18nGroovyQuickFixHandler
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.util.IncorrectOperationException
import org.jetbrains.plugins.grails.fileType.GspFileType

class GrailsGroovyI18nIntentionTest : LightJavaCodeInsightFixtureTestCase() {

  private fun doTest(text: String, defaultPropertyValue: String? = null, args: String = "") {
    val file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, text)

    val intentions = myFixture.filterAvailableIntentions("Extract")
    if (intentions.isEmpty()) {
      assertNull(defaultPropertyValue)
      return
    }

    try {
      GrailsI18nGroovyQuickFixHandler.INSTANCE.checkApplicability(file, myFixture.editor)
    }
    catch (e: IncorrectOperationException) {
      assertNull(defaultPropertyValue)
      return
    }

    val pair = GrailsI18nGroovyQuickFixHandler.calculatePropertyValue(myFixture.editor, file)
    assertNotNull(pair)

    assertEquals(defaultPropertyValue, pair!!.first)
    assertEquals(args, pair.second)
  }

  fun testIntention1() {
    doTest("""${'$'}{"<caret>aaa"}""", "aaa")
  }

  fun testIntention2() {
    doTest("""${'$'}{"<selection>aaa 12</selection>"}""", "aaa 12")
  }

  fun testIntention3() {
    doTest("""${'$'}{<selection>"aaa"</selection>}""", "aaa")
  }

  fun testIntention4() {
    doTest("\${\"\"\"<selection>aaa</selection>\"\"\"}", "aaa")
  }

  fun testIntention5() {
    doTest("\${<selection>\"\"\"aaa\"\"\"</selection>}", "aaa")
  }

  fun testIntention6() {
    doTest("""${'$'}{'<selection>aaa</selection>'}""", "aaa")
  }

  fun testIntention7() {
    doTest("""${'$'}{<selection>'aaa'</selection>}""", "aaa")
  }

  fun testIntentionFalse() {
    doTest("""${'$'}{'a<selection>a</selection>a'}""", null)
  }

  fun testIntentionGString1() {
    doTest("""${'$'}{<selection>'a${'$'}{777}aa'</selection>}""", "a${'$'}{777}aa")
  }

  fun testIntentionGString2() {
    doTest("\${<selection>\"\"\"a${'$'}{777}a\${888}a\"\"\"</selection>}", "a{0}a{1}a", "777, 888")
  }

  fun testIntentionGString3() {
    doTest("\${<selection>\"\"\"a\${777}a\${888}a\"\"\"</selection>}", "a{0}a{1}a", "777, 888")
  }

  fun testIntentionGString4() {
    doTest("""${'$'}{"<caret>a${'$'}{777}a${'$'}{888}a"}""", "a{0}a{1}a", "777, 888")
  }

  fun testIntentionGStringExpression() {
    doTest("""${'$'}{"<caret>a${'$'}a a"}""", "a{0} a", "a")
  }

  fun testIntentionGStringSum() {
    doTest("""${'$'}{"<caret>a${'$'}{a + 1} a"}""", "a{0} a", "a + 1")
  }
}