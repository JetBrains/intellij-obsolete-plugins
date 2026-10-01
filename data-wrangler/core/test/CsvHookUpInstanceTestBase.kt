package com.intellij.dataWrangler.core.test

import com.intellij.database.csv.CsvFileType
import com.intellij.database.csv.CsvFormatResolverCore
import com.intellij.database.datagrid.CsvDocumentDataHookUp
import com.intellij.database.datagrid.GridRequestSource
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.junit5.fixture.disposableFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.ui.EDT
import org.junit.jupiter.api.Assertions

sealed class CsvHookUpInstanceTestBase {
  private fun getCsvHookUpWithContent(text: String): CsvDocumentDataHookUp {
    lateinit var hookUp: CsvDocumentDataHookUp
    runInEdtAndWait {
      val file = PsiFileFactory.getInstance(sharedProject.get()).createFileFromText(
        "abc.csv", CsvFileType.INSTANCE, text
      )
      Assertions.assertEquals(CsvFileType.INSTANCE, file.fileType)
      val document = file.viewProvider.document!!
      val csvFormat = CsvFormatResolverCore.getMoreSuitableCsvFormat(text)!!
      hookUp = CsvDocumentDataHookUp(sharedProject.get(), csvFormat, document, null)
    }

    return hookUp
  }

  internal fun loadCsvDataHookUp(csvDataHookUp: CsvDocumentDataHookUp) {
    invokeAndWaitIfNeeded {
      csvDataHookUp.loader.loadFirstPage(GridRequestSource(null))
      EDT.dispatchAllInvocationEvents()
      csvDataHookUp.awaitParsingFinished()
      EDT.dispatchAllInvocationEvents()
    }
  }

  protected fun getRegisteredCsvHookUpWithContent(text: String): CsvDocumentDataHookUp {
    val hookUp = getCsvHookUpWithContent(text)
    Disposer.register(classLevelDisposable.get(), hookUp)
    return hookUp
  }

  private val sharedProject = projectFixture()
  private val classLevelDisposable = disposableFixture()
}