/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.groovy.grails.i18n

import com.intellij.codeInspection.ex.InspectionToolWrapper
import com.intellij.groovy.grails.i18n.GrailsI18nInspection
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import org.jetbrains.plugins.groovy.grails.GrailsTestCase

class GrailsI18nInspectionTest : GrailsTestCase() {

  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(GrailsI18nInspection::class.java)
  }

  private fun setIgnoreIfDefault(value: Boolean) {
    val tool = InspectionProjectProfileManager.getInstance(project).currentProfile.getInspectionTool(
      GrailsI18nInspection().shortName, project)
    val inspection = (tool as InspectionToolWrapper).tool as GrailsI18nInspection

    inspection.ignoreTagsWithDefault = value
  }

  fun testCodeReference() {
    myFixture.addFileToProject("grails-app/i18n/messages.properties", """
aaa1=Aaaa1
aaa2=Aaaa2
bbb=Bbbb
""")

    myFixture.addFileToProject("grails-app/views/a.gsp", "<g:message code='aa<caret>' />")

    assertEquals(listOf("aaa1", "aaa2"), myFixture.getCompletionVariants("grails-app/views/a.gsp"))
  }

  fun testHighlightingGsp() {
    setIgnoreIfDefault(true)

    myFixture.addFileToProject("grails-app/i18n/messages.properties", """
aaa.bbb=Aaa bbb
""")

    val gsp = addView("aaa.gsp", """
<g:message code="aaa.bbb" />
<g:message code="<error descr="Cannot resolve property key">aaa.bbb.sss</error>" />
<g:message code="aaa.${"$"}{xxx}" />
<g:message code="asdaskdjaskdaj" default="Default Text" />

<<error descr="Element tooltip:tip is not allowed here">tooltip:tip</error> code="sdfsdfsdf" />

<g:sortableColumn titleKey="<error descr="Cannot resolve property key">sdfsdfsdf</error>" />
<g:sortableColumn titleKey="sdfsdfsdf" title="Default Text" />

<%--suppress InvalidI18nProperty --%>
<g:sortableColumn titleKey="sdfsdfsdf" />
""")

    myFixture.testHighlighting(true, false, true, gsp.virtualFile)
  }

  fun testSuppressionWarningForFile() {
    val gsp = addView("aaa.gsp", """
<%--suppress InvalidI18nProperty --%>

<div />
<g:sortableColumn titleKey="sdfsdfsdf" />
""")

    myFixture.testHighlighting(true, false, true, gsp.virtualFile)
  }

  fun testSuppressionAllWarningInFile() {
    val gsp = addView("aaa.gsp", """
<%--suppress ALL --%>

<div />
<g:sortableColumn titleKey="sdfsdfsdf" />
""")

    myFixture.testHighlighting(true, false, true, gsp.virtualFile)
  }

  fun testHighlightingGroovy() {
    setIgnoreIfDefault(true)

    myFixture.addFileToProject("grails-app/i18n/messages.properties", """
aaa.bbb=Aaa bbb
""")

    val file = addController("""
class CccController {
  def index = {
    g.message(code:"aaa.bbb")
    g.message(code:"<error descr="Cannot resolve property key">aaa.bbb.sss</error>")
    g.message(code:"aaa.${'$'}{xxx}")
    g.message(code:"asdaskdjaskdaj", default:"Default Text")

    g.sortableColumn(titleKey:"<error descr="Cannot resolve property key">sdfsdfsdf</error>")
    g.sortableColumn(titleKey:"sdfsdfsdf", title:"Default Text")

    //noinspection InvalidI18nProperty
    g.sortableColumn(titleKey:"sdfsdfsdf")
  }

  @SuppressWarnings("InvalidI18nProperty")
  def zzz = {
    g.sortableColumn(titleKey:"sdfsdfsdf")
  }
}
""")

    myFixture.testHighlighting(true, false, true, file.virtualFile)
  }

  fun testHighlighting2() {
    setIgnoreIfDefault(false)

    val file = addController("""
class CccController {
  def index = {
    g.sortableColumn(titleKey:"<error>sdfsdfsdf</error>", title:"Default Text")
  }
}
""")

    myFixture.testHighlighting(true, false, true, file.virtualFile)
  }

  fun testCreatePropertyGsp() {
    setIgnoreIfDefault(false)

    myFixture.addFileToProject("grails-app/i18n/messages.properties", """
aaa.bbb=Aaa bbb
""")

    configureByView("aaa.gsp", "<g:message code='aaa.bbb.ccc<caret>' />")

    val intentions = myFixture.filterAvailableIntentions("Create property")
    assertSize(1, intentions)

    assertEmpty(myFixture.filterAvailableIntentions("Don't check"))
  }

  fun testCreatePropertyGsp2() {
    setIgnoreIfDefault(false)

    myFixture.addFileToProject("grails-app/i18n/messages.properties", """
aaa.bbb=Aaa bbb
""")

    configureByView("aaa.gsp", "<g:message code='aaa.bbb.ccc<caret>' default='aaa'/>")

    assertSize(1, myFixture.filterAvailableIntentions("Create property"))
    assertSize(1, myFixture.filterAvailableIntentions("Don't check"))
  }
}
