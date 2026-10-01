package com.intellij.dataWrangler.jupyterPython.operations.custom

import com.intellij.dataWrangler.annotations.CommandParameterName
import com.intellij.dataWrangler.annotations.DWCollectionItemType
import com.intellij.dataWrangler.annotations.DWEmptyType
import com.intellij.dataWrangler.annotations.FloatValuesProvider
import com.intellij.dataWrangler.annotations.IntValuesProvider
import com.intellij.dataWrangler.annotations.StringValuesProvider
import com.intellij.dataWrangler.impl.operations.DWTypeDesc
import com.intellij.dataWrangler.impl.operations.DWTypeDesc.DWStructTypeDesc.Field
import com.intellij.dataWrangler.impl.operations.DWTypeDesc.DWStructTypeDesc.FieldAccessor
import com.intellij.dataWrangler.impl.operations.DWTypes
import kotlin.reflect.KClass


internal class FakeTypeBuilder<T: Any>(initialType: KClass<T>) {
  var targetType: KClass<T> = initialType
    private set
  private val annotations: MutableList<Annotation> = mutableListOf()
  private var defValue: T? = null

  constructor(v: T): this(getSupportedType(v))

  fun buildDesc(): DWTypeDesc<T> =
    DWTypes.createFake(targetType, annotations)

  fun buildField(id: String): Field<JupyterCustomCommandParameters, T> =
    Field(id, buildDesc(), PropsAccessor(id, targetType, defValue!!))

  @Suppress("UNCHECKED_CAST")
  fun <U: Any> setTargetType(targetType: KClass<U>): FakeTypeBuilder<U> =
    (this as FakeTypeBuilder<U>).apply {
      this.targetType = targetType
    }

  fun setTypeAnno(type: Annotation): FakeTypeBuilder<T> =
    addAnnotation(type)

  fun addAnnotation(anno: Annotation): FakeTypeBuilder<T> = apply {
    annotations += anno
  }

  fun setDefault(def: T?): FakeTypeBuilder<T> = apply {
    defValue = def
  }

  fun setDisplayName(displayName: String): FakeTypeBuilder<T> =
    addAnnotation(CommandParameterName(displayName))

  @Suppress("UNCHECKED_CAST")
  inline fun <reified U: Any> tryCast(): FakeTypeBuilder<U>? =
    if (targetType == U::class) this as FakeTypeBuilder<U> else null
}


@Suppress("UNCHECKED_CAST")
private fun <T : Any> getSupportedType(v: T): KClass<T> = when (v) {
  is Int -> Int::class
  is Float -> Float::class
  is Double -> Double::class
  is String -> String::class
  is Boolean -> Boolean::class
  is List<*> -> List::class
  is Set<*> -> Set::class
  else -> throw AssertionError("Unsupported type ${v::class} (${v})")
} as KClass<T>

internal fun FakeTypeBuilder<String>.setPossibleValues(possibleValues: Array<String>): FakeTypeBuilder<String> =
  addAnnotation(StringValuesProvider(*possibleValues))
internal fun FakeTypeBuilder<Int>.setPossibleValues(possibleValues: IntArray): FakeTypeBuilder<Int> =
  addAnnotation(IntValuesProvider(*possibleValues))
internal fun FakeTypeBuilder<Float>.setPossibleValues(possibleValues: FloatArray): FakeTypeBuilder<Float> =
  addAnnotation(FloatValuesProvider(*possibleValues))
internal fun <C: Collection<*>> FakeTypeBuilder<C>.setItemDesc(itemDesc: DWTypeDesc<*>?): FakeTypeBuilder<C> =
  addAnnotation(DWCollectionItemType(itemDesc?.anyDefType ?: DWEmptyType::class))

private class PropsAccessor<T: Any>(val id: String, override val type: KClass<T>, val def: T): FieldAccessor<JupyterCustomCommandParameters, T> {
  override fun set(instance: JupyterCustomCommandParameters, value: T) {
    if (value != def) {
      instance.props[id] = value
    }
    else {
      instance.props.remove(id)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun get(instance: JupyterCustomCommandParameters): T {
    return instance.props[id] as? T ?: def
  }
}
