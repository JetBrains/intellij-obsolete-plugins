package com.intellij.dataWrangler.jupyterPython.test.engine

import com.intellij.dataWrangler.jupyterPython.engine.DW_VARIABLE_NAME
import com.intellij.dataWrangler.jupyterPython.engine.JupyterPyDataWranglerNotebookContext
import com.intellij.psi.tree.IElementType
import com.intellij.scientific.tables.api.DSTableCommandExecutor
import com.intellij.scientific.tables.api.DSTableDataRetrieverFromDataSource
import com.intellij.scientific.tables.api.DisposableDSPanelInfo
import com.intellij.scientific.tables.api.OutputPsiExpression
import com.intellij.scientific.tables.panel.DSTableWithStatistics
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.jetbrains.python.PyElementTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestApplication
class JupyterPyDataWranglerNotebookContextTest {
  private val project by projectFixture()

  /**
   * Helper method to test getInitializationCode with different expressions and element types
   */
  private fun testGetInitializationCode(
    expression: String?,
    elementType: IElementType?,
    expectedResult: String,
  ) {
    val mockPanelInfo = mock<DisposableDSPanelInfo> {
      on { outputExpression } doReturn if (expression != null) OutputPsiExpression(expression, elementType) else null
    }

    val mockDataRetriever = mock<DSTableDataRetrieverFromDataSource> {
      onGeneric { panelInfo } doReturn mockPanelInfo
      on { initialTableExpression } doReturn "Out[1]"
    }

    val context = JupyterPyDataWranglerNotebookContext(
      project,
      mock<DSTableWithStatistics>(),
      mockDataRetriever,
      mock<DSTableCommandExecutor>(),
      null
    )
    val result = context.getInitializationCode()

    // Verify the result
    assertEquals(expectedResult, result)
  }

  @Test
  fun `test getInitializationCode with reference expression`() {
    val variableName = "df1"
    // For REFERENCE_EXPRESSION, it should add .copy()
    testGetInitializationCode(
      variableName,
      PyElementTypes.REFERENCE_EXPRESSION,
      "${DW_VARIABLE_NAME} = ${variableName}.copy()"
    )
  }

  @Test
  fun `test getInitializationCode with call expression`() {
    val callExpression = "createDataframe()"
    // For CALL_EXPRESSION, it should use the expression as is
    testGetInitializationCode(
      callExpression,
      PyElementTypes.CALL_EXPRESSION,
      "${DW_VARIABLE_NAME} = ${callExpression}"
    )
  }

  @Test
  fun `test getInitializationCode with qualified call expression`() {
    val qualifiedCallExpression = "bla.createDataframe()"
    // For CALL_EXPRESSION, it should use the expression as is
    testGetInitializationCode(
      qualifiedCallExpression,
      PyElementTypes.CALL_EXPRESSION,
      "${DW_VARIABLE_NAME} = ${qualifiedCallExpression}"
    )
  }

  @Test
  fun `test getInitializationCode with other element type`() {
    val expression = "some_expression"
    // For other element types, it should use the expression as is
    testGetInitializationCode(
      expression,
      PyElementTypes.EXPRESSION_STATEMENT,
      "${DW_VARIABLE_NAME} = ${expression}"
    )
  }

  @Test
  fun `test getInitializationCode with null output expression`() {
    // For a null output expression, it should use a placeholder
    testGetInitializationCode(
      null,
      null,
      "${DW_VARIABLE_NAME} = ${DW_VARIABLE_NAME}"
    )
  }
}