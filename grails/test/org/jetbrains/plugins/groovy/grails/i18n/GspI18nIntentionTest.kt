/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.groovy.grails.i18n

import com.intellij.groovy.grails.i18n.GrailsI18nQuickFixHandler
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.util.IncorrectOperationException
import org.jetbrains.plugins.grails.fileType.GspFileType

class GspI18nIntentionTest : LightJavaCodeInsightFixtureTestCase() {

  private fun doTest(text: String, defaultPropertyValue: String? = null, args: String? = null) {
    val file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, text)

    val intentions = myFixture.filterAvailableIntentions("Extract")
    if (intentions.isEmpty()) {
      assertNull(defaultPropertyValue)
      return
    }

    try {
      GrailsI18nQuickFixHandler.INSTANCE.checkApplicability(file, myFixture.editor)
    }
    catch (e: IncorrectOperationException) {
      assertNull(defaultPropertyValue)
      return
    }

    val pair = GrailsI18nQuickFixHandler.calculatePropertyValue(myFixture.editor, file)
    assertNotNull(pair)

    assertEquals(defaultPropertyValue, pair!!.first)
    assertEquals(args, pair.second)
  }

  fun testIntention1() {
    doTest(
      """
a<selection>aa
sda</selection>
""", "aa\nsda", ""
    )
  }

  fun testIntention2() {
    doTest(
      """
a<selection>aa<% out << 1 %>sda</selection>
"""
    )
  }

  fun testIntention21() {
    doTest("""
a<selection>aasda${'$'}{</selection>2}
""")
  }

  fun testIntention22() {
    doTest("""
a${'$'}{1<selection>}aasda</selection>
""")
  }

  fun testIntention3() {
    doTest("a<selection><div>aasda</div></selection>", "<div>aasda</div>", "")
  }

  fun testIntention4() {
    doTest("a<selection><g:link/></selection>")
  }

  fun testIntention5() {
    doTest("a<caret>aa")
  }

  fun testIntention6() {
    doTest("a<% 1 %><selection>${'$'}{aaa}</selection><% out << \"aaa\" %>", "{0}", "aaa")
  }

  fun testIntention7() {
    doTest("a<selection>a ${'$'}{aaa} s <%= 777 * 5   %> </selection> sdsa", "a {0} s {1} ", "aaa, 777 * 5")
  }

  fun testIntention8() {
    doTest("<selection>aaa${'$'}{777}bbb${'$'}{'!' + actionName + 'aaa'}</selection>", "aaa{0}bbb{1}", "777, '!' + actionName + 'aaa'")
  }

  fun testIntention9() {
    doTest("<selection>aaa${'$'}{777}${'$'}{777}</selection>", "aaa{0}{1}", "777, 777")
  }

  fun testIntention10() {
    doTest("<input class=\"button green medium\" type=\"button\" value=\"<selection>Subscribe</selection>\"/>", "Subscribe", "")
  }

  fun testIntention11() {
    doTest("<input class=\"button green medium\" type=\"button\" value=\"<selection>Subscribe ${'$'}{aaa}</selection>\"/>", "Subscribe {0}", "aaa")
  }

  fun testIntention12() {
    doTest("<input class=\"button green medium\" type=\"button\" <selection>value=\"Subscribe</selection>\"/>")
  }
}
