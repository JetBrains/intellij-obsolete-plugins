package com.intellij.dbt.test.run

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.dbt.DbtBundle
import com.intellij.dbt.test.DbtTestCase

class DbtRunLineMarkerContributorTest : DbtTestCase() {
  fun testLineMarker() {
    val filePath = "models/model_a.sql"
    myFixture.configureByFile(filePath)
    myFixture.doHighlighting()
    val document = myFixture.editor.document
    val lineMarker = DaemonCodeAnalyzerImpl.getLineMarkers(document, myFixture.project).firstOrNull()
    assertNotNull(lineMarker)
    assertEquals(DbtBundle.message("dbt.run.line.marker.tooltip"), lineMarker!!.lineMarkerTooltip)
    assertEquals(0, document.getLineNumber(lineMarker.startOffset))
  }

  fun testNoLineMarkerUnderDbtPackageDirectory() {
    val filePath = "dbt_packages/my_file.sql"
    myFixture.configureByFile(filePath)
    myFixture.doHighlighting()
    val document = myFixture.editor.document
    val lineMarker = DaemonCodeAnalyzerImpl.getLineMarkers(document, myFixture.project).firstOrNull()
    assertNull(lineMarker)
  }

  override fun getBasePath(): String = "${super.getBasePath()}/completion"
}