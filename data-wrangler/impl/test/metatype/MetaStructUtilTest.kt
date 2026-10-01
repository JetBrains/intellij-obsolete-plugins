package com.intellij.dataWrangler.impl.test.metatype

import com.intellij.dataWrangler.annotations.DisplayName
import com.intellij.dataWrangler.annotations.IntValuesProvider
import com.intellij.dataWrangler.impl.operations.DWTypeDesc
import com.intellij.dataWrangler.impl.operations.DWTypes
import com.intellij.dataWrangler.impl.operations.FieldTypeImpl
import com.intellij.dataWrangler.impl.operations.MetaStructImpl
import com.intellij.dataWrangler.impl.operations.toMetaStructure
import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.annotations.Nls
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.function.Supplier

internal data class ExampleParameters(var intField: Int = 0)

internal data class ExampleFieldParameters(@IntValuesProvider(1, 2, 3) var intField: Int = 30, var string: String = "")

internal data class ExampleEnumParameters(var intField: Int = 0, var enumField: MyEnum = MyEnum.ONE) {
  enum class MyEnum(override val displayNamePtr: Supplier<@Nls String>) : DisplayName.Impl {
    ONE({ "one" }), TWO({ "two" }), THREE({ "three" })
  }
}

@TestApplication
class MetaStructUtilTest {

  private val oldVal = 30
  private val newVal = 50
  private val exampleInstance = ExampleParameters(oldVal)
  private val exampleFieldParameters = ExampleFieldParameters()
  private val exampleEnumParameters = ExampleEnumParameters()
  private val id = "class com.intellij.dataWrangler.impl.test.metatype.ExampleParameters#intField"
  private val idField = "class com.intellij.dataWrangler.impl.test.metatype.ExampleFieldParameters#intField"
  private val idString = "class com.intellij.dataWrangler.impl.test.metatype.ExampleFieldParameters#string"
  private val idEnum = "class com.intellij.dataWrangler.impl.test.metatype.ExampleEnumParameters#enumField"

  @Test
  fun testStructure() {
    val primitiveType = FieldTypeImpl(DWTypeDesc.DWStructTypeDesc.createField(ExampleParameters::intField))
    val expectedMetaStruct = MetaStructImpl.create<ExampleParameters>(
      "class com.intellij.dataWrangler.impl.test.metatype.ExampleParameters",
      DWTypes.create(ExampleParameters::class) as DWTypeDesc.DWStructTypeDesc<ExampleParameters>,
      mapOf(id to primitiveType)
    )
    val metaStruct = toMetaStructure(exampleInstance::class)
    Assertions.assertNotNull(metaStruct)
    Assertions.assertTrue(metaStruct.id == expectedMetaStruct.id &&
                          metaStruct.dataClassType == expectedMetaStruct.dataClassType)
  }

  @Test
  fun testSetter() {
    val metaStruct = toMetaStructure(exampleInstance::class)
    Assertions.assertNotNull(metaStruct)
    val field = (metaStruct.getFields()[id] as? FieldTypeImpl<Int, ExampleParameters>)
    Assertions.assertNotNull(field)
    Assertions.assertEquals(oldVal, field?.get(exampleInstance))
    field?.set(exampleInstance, newVal)
    Assertions.assertEquals(newVal, field?.get(exampleInstance))
  }

  @Test
  fun testFieldWrapper() {
    val metaStruct = toMetaStructure(exampleFieldParameters::class)
    Assertions.assertNotNull(metaStruct)
    val field = metaStruct.getFields()[idField] as? FieldTypeImpl<Int, ExampleFieldParameters>
    Assertions.assertNotNull(field)
    Assertions.assertEquals(field?.get(exampleFieldParameters), oldVal)
  }

  @Test
  fun testFieldWrapperSetter() {
    val metaStruct = toMetaStructure(exampleFieldParameters::class)
    Assertions.assertNotNull(metaStruct)
    val field = metaStruct.getFields()[idField] as? FieldTypeImpl<Int, ExampleFieldParameters>
    Assertions.assertNotNull(field)
    Assertions.assertEquals(field?.get(exampleFieldParameters), oldVal)
    field?.set(exampleFieldParameters, newVal)
    Assertions.assertEquals(field?.get(exampleFieldParameters), newVal)
  }

  @Test
  fun testFieldWrapperGetter() {
    val metaStruct = toMetaStructure(exampleFieldParameters::class)
    Assertions.assertNotNull(metaStruct)
    val field = metaStruct.getFields()[idField] as? FieldTypeImpl<Int, ExampleFieldParameters>
    val fieldString = metaStruct.getFields()[idString] as? FieldTypeImpl<String, ExampleFieldParameters>
    Assertions.assertEquals(field?.get(exampleFieldParameters), oldVal)
    Assertions.assertEquals(fieldString?.get(exampleFieldParameters), "")
  }

  @Test
  fun testEnumField() {
    val metaStruct = toMetaStructure(exampleEnumParameters::class)
    val field = metaStruct.getFields()[idEnum] as? FieldTypeImpl<ExampleEnumParameters.MyEnum, ExampleEnumParameters>
    Assertions.assertNotNull(field)
    Assertions.assertEquals(field?.get(exampleEnumParameters), ExampleEnumParameters.MyEnum.ONE)

    field?.set(exampleEnumParameters, ExampleEnumParameters.MyEnum.TWO)
    Assertions.assertEquals(field?.get(exampleEnumParameters), ExampleEnumParameters.MyEnum.TWO)
  }
}