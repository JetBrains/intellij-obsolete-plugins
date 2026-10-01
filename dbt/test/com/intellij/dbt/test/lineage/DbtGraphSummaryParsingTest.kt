package com.intellij.dbt.test.lineage

import com.intellij.dbt.diagram.readLineage
import com.intellij.dbt.test.DbtTestCase
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy
import java.nio.file.Files

class DbtGraphSummaryParsingTest : DbtTestCase() {
  fun testJaffleShopExample() {
    val path =  FileUtil.join(IdeaTestExecutionPolicy.getHomePathWithPolicy(), "plugins", "dbt", "testData", "lineage", "graph_summary.json")
    val graphSummaryContent: String = Files.readString(path.toNioPathOrNull()!!)
    val lineage = readLineage(graphSummaryContent, module)
    assertNotNull(lineage)
    assertEquals(8, lineage!!.nodes().size)
    assertEquals(8, lineage.nodes().size)
    assertEquals(8, lineage.edges().size)
  }
}