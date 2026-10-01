package com.intellij.dataWrangler.core.test

import com.intellij.dataWrangler.core.engine.DataWranglerCoreContext
import com.intellij.testFramework.junit5.TestApplication
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@TestApplication
data object DataWranglerCoreContextTest : CsvHookUpInstanceTestBase() {
  @Test
  fun `test getText() method`() {
    val csvDataHookUp = getRegisteredCsvHookUpWithContent("Name,Age,City\n" +
                                                          "John,25,New York\n" +          // 0
                                                          "Emily,30,Los Angeles\n" +      // 1
                                                          "Michael,35,Chicago\n" +        // 2
                                                          "Sarah,28,Houston\n" +          // 3
                                                          "David,32,Miaim\n" +            // 4
                                                          "Jessica,27,San Francisco\n" +  // 5
                                                          "Daniel,40,Dallas\n" +          // 6
                                                          "Olivia,33,Seattle\n")          // 7

    loadCsvDataHookUp(csvDataHookUp)
    Assertions.assertEquals("John", DataWranglerCoreContext.getText(0, 0, csvDataHookUp))
    Assertions.assertEquals("Michael", DataWranglerCoreContext.getText(2, 0, csvDataHookUp))
    Assertions.assertEquals("28", DataWranglerCoreContext.getText(3, 1, csvDataHookUp))
    Assertions.assertEquals("Seattle", DataWranglerCoreContext.getText(7, 2, csvDataHookUp))
  }

  @Test
  fun `test deleteRows() method`() {
    val csvDataHookUp = getRegisteredCsvHookUpWithContent("Name,Age,City\n" +
                                                          "John,25,New York\n" +          // 0
                                                          "Emily,30,Los Angeles\n" +      // 1
                                                          "Michael,35,Chicago\n" +        // 2
                                                          "Sarah,28,Houston\n" +          // 3
                                                          "David,32,Miaim\n" +            // 4
                                                          "Jessica,27,San Francisco\n" +  // 5
                                                          "Daniel,40,Dallas\n" +          // 6
                                                          "Olivia,33,Seattle\n")          // 7

    loadCsvDataHookUp(csvDataHookUp)
    DataWranglerCoreContext.deleteRows(listOf(0, 1, 2), csvDataHookUp)
    loadCsvDataHookUp(csvDataHookUp)
    Assertions.assertEquals("Sarah", DataWranglerCoreContext.getText(0, 0, csvDataHookUp))
    Assertions.assertEquals("David", DataWranglerCoreContext.getText(1, 0, csvDataHookUp))
    Assertions.assertEquals("Jessica", DataWranglerCoreContext.getText(2, 0, csvDataHookUp))
    Assertions.assertEquals("Daniel", DataWranglerCoreContext.getText(3, 0, csvDataHookUp))
    Assertions.assertEquals("Olivia", DataWranglerCoreContext.getText(4, 0, csvDataHookUp))
  }
}