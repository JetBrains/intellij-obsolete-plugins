package com.intellij.dbt.test

import com.intellij.dbt.codeInsight.DbtModelRefReferenceProvider
import com.intellij.jinja.Jinja2FileType
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.sql.psi.SqlReferenceExpression
import com.intellij.util.ProcessingContext

class DbtResolveTest : DbtTestCase() {
  private val dbtRefResolver = DbtModelRefReferenceProvider()
  private val context : ProcessingContext = ProcessingContext()

  fun testFindReferenceInSameFolder() { doTest("models", 1) }

  fun testFindReferenceInSubFolder() { doTest("models/subModelsDir", 2) }

  fun testFindReferenceInSubSubFolder() { doTest("models/subModelsDir/subSubModelsDir", 3) }

  fun testFindReferenceInSeedsFolder() { doTest("seeds", 1, true) }

  fun testSqlColumnResolve() {
    myFixture.configureByFiles("models/model_a.sql", "models/model_b.sql")
    val element = myFixture.file.findElementAt(myFixture.caretOffset)
    val referenceElement = PsiTreeUtil.getParentOfType(element, SqlReferenceExpression::class.java)
    assertNotNull(referenceElement)
    val resolved = referenceElement!!.resolve()
    assertNotNull(resolved)
    assertEquals("first_name", resolved!!.text)
    assertEquals("model_b.sql", resolved.containingFile.name)
  }

  fun testSeedColumnResolve() {
    myFixture.configureByFiles("models/model_c.sql", "seeds/seed.csv")
    val element = myFixture.file.findElementAt(myFixture.caretOffset)
    val referenceElement = PsiTreeUtil.getParentOfType(element, SqlReferenceExpression::class.java)
    assertNotNull(referenceElement)
    val resolved = referenceElement!!.resolve()
    assertNotNull(resolved)
    assertEquals("first_name", resolved!!.text)
    assertEquals("seed.csv", resolved.containingFile.name)
  }

  fun testSeveralSelectQueriesInModel() {
    myFixture.configureByFiles("models/model_d.sql", "models/model_e.sql")
    val element = myFixture.file.findElementAt(myFixture.caretOffset)
    val referenceElement = PsiTreeUtil.getParentOfType(element, SqlReferenceExpression::class.java)
    assertNotNull(referenceElement)
    val resolved = referenceElement!!.resolve()
    assertNotNull(resolved)
    assertEquals("last_name", resolved!!.text)
    assertEquals("model_e.sql", resolved.containingFile.name)
  }

  fun testNoResolveDeeperAsLimit() {
    doTest("models/model_f.sql", "models/level_01/level_02/level_03/level_04/level_05/level_06/level_07/level_08/level_09/level_10/deep_model.sql", false)
  }

  fun testResolveDeepModelButInLimit() {
    doTest("models/model_g.sql", "models/level_01/level_02/level_03/level_04/level_05/level_06/level_07/level_08/not_so_deep_model.sql", true)
  }

  private fun getExtension(inSeedsDirectory: Boolean) = if (inSeedsDirectory) "csv" else "sql"

  private fun doTest(referenceDirPath: String, testIndex: Int, inSeedsDirectory: Boolean = false) {
    val files = myFixture.configureByFiles("$referenceDirPath/second$testIndex.${getExtension(inSeedsDirectory)}")
    myFixture.configureByText(Jinja2FileType.INSTANCE, "select * from {{ re<caret>f('second$testIndex') }}")
    val referencedModel = files.getOrNull(0)
    assertNotNull(referencedModel)
    val refFunctionCall =  PsiTreeUtil.getParentOfType(myFixture.file.findElementAt(myFixture.caretOffset), Jinja2FunctionCall::class.java)
    assertNotNull(refFunctionCall)
    val referenceToModel = dbtRefResolver.getReferencesByElement(refFunctionCall as PsiElement, context).firstOrNull()?.resolve()
    assertEquals(referencedModel, referenceToModel)
  }

  private fun doTest(filePath: String, referencedModelPath: String, shouldBeResolved: Boolean) {
    myFixture.configureByFiles(filePath, referencedModelPath)

    val element = myFixture.file.findElementAt(myFixture.caretOffset)
    assertNotNull(element)

    val jinjaCall = PsiTreeUtil.getParentOfType(element, Jinja2FunctionCall::class.java)
    assertNotNull(jinjaCall)

    val references = dbtRefResolver.getReferencesByElement(jinjaCall!!, context)
    assertTrue(references.isNotEmpty())

    val resolved = references[0].resolve()
    if (shouldBeResolved) {
      assertNotNull(resolved)
      assertTrue(referencedModelPath.endsWith(resolved!!.containingFile.name))
    } else {
      assertNull(resolved)
    }
  }

  override fun getBasePath(): String = "${super.getBasePath()}/resolve"
}