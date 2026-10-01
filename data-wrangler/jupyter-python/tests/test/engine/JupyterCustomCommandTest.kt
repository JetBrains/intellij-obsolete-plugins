package com.intellij.dataWrangler.jupyterPython.test.engine

import com.intellij.dataWrangler.DWParameterPanelTestUtil
import com.intellij.dataWrangler.executor.DataWranglerEngine
import com.intellij.dataWrangler.impl.operations.FieldTypeImpl
import com.intellij.dataWrangler.impl.operations.setDefaultsFromPossibleValues
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.jupyterPython.operations.custom.JupyterCustomCommandFactories
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.database.testFramework.asVirtualFile
import com.intellij.database.testFramework.dbTestDataHelper
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.use
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.UsefulTestCase.assertSameLinesWithFile
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.runInEdtAndWait
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

@TestDataPath("\$PROJECT_ROOT/plugins/data-wrangler/jupyter-python/tests/testData/customCommands")
@TestApplication
class JupyterCustomCommandTest {
  companion object {
    val testData = dbTestDataHelper<JupyterCustomCommandTest>()

    @JvmStatic
    fun listCommandFiles(): List<Path> =
      testData.listRelFiles().filter { it.extension == "py" }.toList()
  }

  private val project by projectFixture()

  @ParameterizedTest
  @MethodSource("listCommandFiles")
  fun checkParameters(file: Path) {
    val commands = JupyterCustomCommandFactories.parseCustomCommandsForTest(project, testData.resolve(file).asVirtualFile())
    val res = buildString {
      commands.forEach { cf ->
        checkCustomFactoryParameters(cf)
      }
    }
    assertSameLinesWithFile(testData.resolve(file.nameWithoutExtension + ".md").toString(), res)
  }

  private fun <P: Any> StringBuilder.checkCustomFactoryParameters(cf: CommandFactory<P, PythonDataWranglerContext>) {
    val mt = cf.parametersMetaType
    val params = mt.getNewInstance()
    mt.setDefaultsFromPossibleValues(params, null)
    val c = cf.createCommand(params)

    append("# ").append(cf.id).append("\n")
    append("* name = ").append(cf.commandName).append("\n")
    append("* description = ").append(cf.getDescription()).append("\n")
    append("* label = ").append(c.getCommandLabel()).append("\n")
    append("* details = ").append(c.getDescription()).append("\n")
    append("* ").append(mt.id).append("\n")
    mt.getFields().forEach { (_, f) ->
      f as FieldTypeImpl
      append("  * ").append(f.id).append("\n")
      append("    * name = ").append(f.desc.defaultName ?: f.id).append("\n")
      append("    * type = ").append(f.getType().simpleName).append("\n")
      append("    * def = ").append(f.get(params)).append("\n")
      append("    * dwtype = ").append(f.desc).append("\n")
      append("    * values = ").append(f.getPossibleValues(null)).append("\n")
    }
  }

  @ParameterizedTest
  @MethodSource("listCommandFiles")
  fun checkUi(file: Path) {
    val commands = JupyterCustomCommandFactories.parseCustomCommandsForTest(project, testData.resolve(file).asVirtualFile())
    val pythonEngine = DataWranglerEngine.EP.extensionList.find { DWParameterPanelTestUtil.isPythonEngine(it) } as DataWranglerEngine<PythonDataWranglerContext>
    val res = buildString {
      runInEdtAndWait {
        Disposer.newDisposable().use { disposable ->
          val model = DWParameterPanelTestUtil.createTestGridModel()
          val session = DWParameterPanelTestUtil.createSession(project, pythonEngine, model, disposable)
          commands.groupBy { it.getGroupName() }.forEach { (g, factories) ->
            DWParameterPanelTestUtil.dumpGroup(session, g, factories, this, 0)
          }
        }
      }
    }
    assertSameLinesWithFile(testData.resolve(file.nameWithoutExtension + ".ui.txt").toString(), res)
  }

  private fun <P: Any> StringBuilder.checkCustomFactoryUi(cf: CommandFactory<P, PythonDataWranglerContext>) {
    val mt = cf.parametersMetaType
    val params = mt.getNewInstance()
    mt.setDefaultsFromPossibleValues(params, null)
    val c = cf.createCommand(params)

    append("# ").append(cf.id).append("\n")
    append("* name = ").append(cf.commandName).append("\n")
    append("* description = ").append(cf.getDescription()).append("\n")
    append("* label = ").append(c.getCommandLabel()).append("\n")
    append("* details = ").append(c.getDescription()).append("\n")
    append("* ").append(mt.id).append("\n")
    mt.getFields().forEach { (_, f) ->
      f as FieldTypeImpl
      append("  * ").append(f.id).append("\n")
      append("    * name = ").append(f.desc.defaultName ?: f.id).append("\n")
      append("    * type = ").append(f.getType().simpleName).append("\n")
      append("    * def = ").append(f.get(params)).append("\n")
      append("    * dwtype = ").append(f.desc).append("\n")
      append("    * values = ").append(f.getPossibleValues(null)).append("\n")
    }
  }
}