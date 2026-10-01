package com.intellij.dataWrangler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.executor.DataWranglerEngine
import com.intellij.dataWrangler.impl.operations.DWTypeDesc
import com.intellij.dataWrangler.impl.operations.MetaStructImpl
import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerEngineUtils
import com.intellij.dataWrangler.jupyterPython.serialisation.SerializableTransformationStep
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.MetaStruct
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.database.testFramework.dbTestDataHelper
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.UsefulTestCase.assertSameLines
import com.intellij.testFramework.UsefulTestCase.assertSameLinesWithFile
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.reflect.full.createInstance
import kotlin.streams.asStream

private val jackson = ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true)

@TestDataPath("\$PROJECT_ROOT/plugins/data-wrangler/tests/testData/serialisation")
@TestApplication
class DWOperationSerialisationTest {
  companion object {
    val project = projectFixture(openAfterCreation = true)
    val testData = dbTestDataHelper<DWOperationSerialisationTest>()

    @JvmStatic
    fun getTestFiles(): Stream<Path> {
      return testData.listRelFilesRecursive().filter { it.name.endsWith(".json") }.asStream()
    }

    @JvmStatic
    fun getCommands(): Stream<Arguments> {
      project.get()
      return DataWranglerEngine.EP.extensionList.asSequence().flatMap { engine ->
        engine.commandsFactories().value.map { Arguments.argumentSet("${it.id.shortId()}[${engine.id}]",  engine.id, it.id) }
      }.asStream()
    }
  }

  @MethodSource("getTestFiles")
  @ParameterizedTest
  fun checkCheckFiles(testFileName: Path) {
    val testFile = testData.resolve(testFileName)
    val deserialized = deserialize(testFile)
    val serialized = serialiseStep(deserialized)
    val upToDate = testFile.name.upToDateName()?.let { testFile.resolveSibling(it) }
    assertSameLinesWithFile((upToDate?.takeIf { Files.exists(it) } ?: testFile).pathString, serialized)
  }

  private fun String.upToDateName(): String? =
    Regex("(\\.old(?:\\.\\w+)?)\\.json$").find(this)?.groups?.get(1)?.let {
      removeRange(it.range)
    }

  private fun deserialize(testFile: Path): TransformationStep<*, *> = jackson.readValue(testFile.toFile(), SerializableTransformationStep::class.java).toTransformationStep()

  @MethodSource("getCommands")
  @ParameterizedTest
  fun checkCommands(engineId: String, commandFactoryId: String) {
    Assumptions.assumeTrue(engineId == PythonDataWranglerEngineUtils.PYTHON_ENGINE_ID)
    checkCommands(DataWranglerEngine.EP.extensionList.find { it.id == engineId }!!, commandFactoryId)
  }

  private fun <C: DataWranglerContext> checkCommands(engine: DataWranglerEngine<C>, commandFactoryId: String) {
    checkCommands(engine, engine.commandsFactories().value.find { it.id == commandFactoryId }!!)
  }

  private fun <C: DataWranglerContext, P: Any> checkCommands(engine: DataWranglerEngine<C>, commandFactory: CommandFactory<P, C>) {
    val testFile = testData.resolve(engine.id).resolve(commandFactory.id.shortId() + ".json")
    val parametersMetaType = commandFactory.parametersMetaType
    val parameters = parametersMetaType.newNonDefaultParameters()
    val original = TransformationStep(commandFactory, parameters)
    val json = serialiseStep(original)
    val deserialized = deserialize(json)
    if (original != deserialized) {
      assertSameLines(json, serialiseStep(deserialized))
      fail { "deserialisation mismatch, but serialisation matches" }
    }
    if (!Files.exists(testFile)) {
      assertSameLinesWithFile(testFile.pathString, json)
    }
  }

  private fun <P : Any> MetaStruct<P>.newNonDefaultParameters(): P {
    val parameters = getNewInstance()
    parameters.setNonDefaultFields((this as MetaStructImpl).desc)
    return parameters
  }

  private fun <P : Any> P.setNonDefaultFields(desc: DWTypeDesc.DWStructTypeDesc<P>) {
    desc.fields.forEach {
      setNonDefaultField(it)
    }
  }

  private fun <P : Any, V : Any> P.setNonDefaultField(field: DWTypeDesc.DWStructTypeDesc.Field<P, V>) {
    val desc = field.desc
    if (desc is DWTypeDesc.DWStructTypeDesc) {
      val obj = field.accessor.get(this)
      obj.setNonDefaultFields(desc)
    }
    else if (desc is DWTypeDesc.DWVariantTypeDesc) {
      val obj = field.accessor.get(this)
      val otherVariant = desc.variants.find { it.type != obj::class }!!
      val newObj = otherVariant.type.createInstance()
      newObj.setNonDefaultFields(otherVariant.desc as DWTypeDesc.DWStructTypeDesc<V>)
    }
    else {
      val curValue = field.accessor.get(this)
      val nonDef = getOtherValue(desc, curValue)
      field.accessor.set(this, nonDef)
    }
  }

  private fun <V : Any> getOtherValue(desc: DWTypeDesc<V>, v: V?): V {
    desc.possibleValues?.getValues(null)?.find { it != v }?.let {
      return it
    }
    if (desc is DWTypeDesc.DWCollectionTypeDesc<*, *>) {
      return when (desc.targetType) {
        List::class -> listOf(getOtherValue(desc.itemType, null))
        Set::class -> setOf(getOtherValue(desc.itemType, null))
        else -> error("Unsupported type ${desc.targetType}")
      } as V
    }
    return when (desc.targetType) {
      Int::class -> v.chooseOtherValue(123, 456)
      Double::class -> v.chooseOtherValue(123.0, 456.0)
      Float::class -> v.chooseOtherValue(123.0f, 456.0f)
      String::class -> v.chooseOtherValue("abc", "def")
      Boolean::class -> v.chooseOtherValue(true, false)
      else -> error("Unsupported type ${desc.targetType} ($desc)")
    } as V
  }

  private fun <V : Any> V?.chooseOtherValue(v1: V, v2: V): V = if (this == v1) v2 else v1
  private fun deserialize(json: String): TransformationStep<*, *> {
    val step = jackson.readValue(json, SerializableTransformationStep::class.java)
    val deserialized = step.toTransformationStep()
    return deserialized
  }

  private fun <C : DataWranglerContext, P : Any> serialiseStep(original: TransformationStep<P, C>): String {
    val step = SerializableTransformationStep.createSerializableTransformationStep(original)
    val json = jackson.writeValueAsString(step)
    return json!!
  }
}


private fun String.shortId(): String {
  return substringAfterLast('.')
}