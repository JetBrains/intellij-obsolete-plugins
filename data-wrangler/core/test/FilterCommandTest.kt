package com.intellij.dataWrangler.core.test

import com.intellij.dataWrangler.core.operations.DataWranglerCommandFilter
import com.intellij.dataWrangler.core.operations.FilterCondition
import com.intellij.dataWrangler.core.operations.FilterParameters
import com.intellij.testFramework.junit5.TestApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@TestApplication
data object FilterCommandTest : CsvHookUpInstanceTestBase() {
  @Test
  fun `test equal several rows left`() {
    val text = "\"sepal.length\",\"sepal.width\",\"petal.length\",\"petal.width\",\"variety\"\n" +
               "5.1,3.5,1.4,.2,\"Setosa\"\n" +
               "4.9,3,1.4,.2,\"Setosa\"\n" +
               "4.7,3.2,1.3,.2,\"Setosa\"\n" +
               "4.6,3.1,1.5,.2,\"NotSetosa\"\n" +
               "5,3.6,1.4,.2,\"Setosa\""

    val textAfter = "\"sepal.length\",\"sepal.width\",\"petal.length\",\"petal.width\",\"variety\"\n" +
                    "5.1,3.5,1.4,.2,\"Setosa\"\n" +
                    "4.9,3,1.4,.2,\"Setosa\"\n" +
                    "4.7,3.2,1.3,.2,\"Setosa\"\n" +
                    "5,3.6,1.4,.2,\"Setosa\""

    val csvDataHookUp = getRegisteredCsvHookUpWithContent(text)
    loadCsvDataHookUp(csvDataHookUp)
    val filterParameters = FilterParameters("variety", "Setosa", FilterCondition.EQUAL)
    runBlocking { DataWranglerCommandFilter.filterTable(filterParameters, csvDataHookUp) }
    loadCsvDataHookUp(csvDataHookUp)
    Assertions.assertEquals(textAfter, csvDataHookUp.document.text)
  }

  @Test
  fun `test equal one row left`() {
    val text = "\"sepal.length\",\"sepal.width\",\"petal.length\",\"petal.width\",\"variety\"\n" +
               "5.1,3.5,1.4,.2,\"Setosa\"\n" +
               "4.9,3,1.4,.2,\"Setosa\"\n" +
               "4.7,3.2,1.3,.2,\"Setosa\"\n" +
               "4.6,3.1,1.5,.2,\"NotSetosa\"\n" +
               "5,3.6,1.4,.2,\"Setosa\""

    val textAfter = "\"sepal.length\",\"sepal.width\",\"petal.length\",\"petal.width\",\"variety\"\n" +
                    "4.6,3.1,1.5,.2,\"NotSetosa\"\n"

    val csvDataHookUp = getRegisteredCsvHookUpWithContent(text)
    loadCsvDataHookUp(csvDataHookUp)
    val filterParameters = FilterParameters("variety", "NotSetosa", FilterCondition.EQUAL)
    runBlocking { DataWranglerCommandFilter.filterTable(filterParameters, csvDataHookUp) }
    loadCsvDataHookUp(csvDataHookUp)
    Assertions.assertEquals(textAfter, csvDataHookUp.document.text)
  }

  @Test
  fun `test greater one row left`() {
    val text = "\"sepal.length\",\"sepal.width\",\"petal.length\",\"petal.width\",\"variety\"\n" +
               "5.1,3.5,1.4,.2,\"Setosa\"\n" +
               "4.9,3,1.4,.2,\"Setosa\"\n" +
               "4.7,3.2,1.3,.2,\"Setosa\"\n" +
               "4.6,3.1,1.5,.2,\"NotSetosa\"\n" +
               "5,3.6,1.4,.2,\"Setosa\""

    val textAfter = "\"sepal.length\",\"sepal.width\",\"petal.length\",\"petal.width\",\"variety\"\n" +
                    "5.1,3.5,1.4,.2,\"Setosa\"\n" +
                    "4.9,3,1.4,.2,\"Setosa\"\n" +
                    "4.7,3.2,1.3,.2,\"Setosa\"\n" +
                    "4.6,3.1,1.5,.2,\"NotSetosa\"\n" +
                    "5,3.6,1.4,.2,\"Setosa\""

    val csvDataHookUp = getRegisteredCsvHookUpWithContent(text)
    loadCsvDataHookUp(csvDataHookUp)
    val filterParameters = FilterParameters("sepal.length", "3", FilterCondition.GREATER)
    runBlocking { DataWranglerCommandFilter.filterTable(filterParameters, csvDataHookUp) }
    loadCsvDataHookUp(csvDataHookUp)
    Assertions.assertEquals(textAfter, csvDataHookUp.document.text)
  }

  @Test
  fun `test greater several rows left`() {
    val text = "\"sepal.length\",\"sepal.width\",\"petal.length\",\"petal.width\",\"variety\"\n" +
               "5.1,3.5,1.4,.2,\"Setosa\"\n" +
               "4,3,1.4,.2,\"Setosa\"\n" +
               "4,3.2,1.3,.2,\"Setosa\"\n" +
               "4,3.1,1.5,.2,\"NotSetosa\"\n" +
               "5,3.6,1.4,.2,\"Setosa\""

    val textAfter = "\"sepal.length\",\"sepal.width\",\"petal.length\",\"petal.width\",\"variety\"\n" +
                    "5.1,3.5,1.4,.2,\"Setosa\"\n" +
                    "5,3.6,1.4,.2,\"Setosa\""

    val csvDataHookUp = getRegisteredCsvHookUpWithContent(text)
    loadCsvDataHookUp(csvDataHookUp)
    val filterParameters = FilterParameters("sepal.length", "4", FilterCondition.GREATER)
    runBlocking { DataWranglerCommandFilter.filterTable(filterParameters, csvDataHookUp) }
    loadCsvDataHookUp(csvDataHookUp)
    Assertions.assertEquals(textAfter, csvDataHookUp.document.text)
  }

  @Test
  fun `test less one row left`() {
    val text = "\"sepal.length\",\"sepal.width\",\"petal.length\",\"petal.width\",\"variety\"\n" +
               "5.1,3.5,1.4,.2,\"Setosa\"\n" +
               "4,3,1.4,.2,\"Setosa\"\n" +
               "3,3.2,1.3,.2,\"Setosa\"\n" +
               "4,3.1,1.5,.2,\"NotSetosa\"\n" +
               "5,3.6,1.4,.2,\"Setosa\""

    val textAfter = "\"sepal.length\",\"sepal.width\",\"petal.length\",\"petal.width\",\"variety\"\n" +
                    "3,3.2,1.3,.2,\"Setosa\"\n"

    val csvDataHookUp = getRegisteredCsvHookUpWithContent(text)
    loadCsvDataHookUp(csvDataHookUp)
    val filterParameters = FilterParameters("sepal.length", "4", FilterCondition.LESS)
    runBlocking { DataWranglerCommandFilter.filterTable(filterParameters, csvDataHookUp) }
    loadCsvDataHookUp(csvDataHookUp)
    Assertions.assertEquals(textAfter, csvDataHookUp.document.text)
  }

  @Test
  fun `test less several rows left`() {
    val text = "\"sepal.length\",\"sepal.width\",\"petal.length\",\"petal.width\",\"variety\"\n" +
               "5.1,3.5,1.4,.2,\"Setosa\"\n" +
               "4,3,1.4,.2,\"Setosa\"\n" +
               "4,3.2,1.3,.2,\"Setosa\"\n" +
               "4,3.1,1.5,.2,\"NotSetosa\"\n" +
               "5,3.6,1.4,.2,\"Setosa\""

    val textAfter = "\"sepal.length\",\"sepal.width\",\"petal.length\",\"petal.width\",\"variety\"\n" +
                    "4,3,1.4,.2,\"Setosa\"\n" +
                    "4,3.2,1.3,.2,\"Setosa\"\n" +
                    "4,3.1,1.5,.2,\"NotSetosa\"\n"

    val csvDataHookUp = getRegisteredCsvHookUpWithContent(text)
    loadCsvDataHookUp(csvDataHookUp)
    val filterParameters = FilterParameters("sepal.length", "5", FilterCondition.LESS)
    runBlocking { DataWranglerCommandFilter.filterTable(filterParameters, csvDataHookUp) }
    loadCsvDataHookUp(csvDataHookUp)
    Assertions.assertEquals(textAfter, csvDataHookUp.document.text)
  }
}