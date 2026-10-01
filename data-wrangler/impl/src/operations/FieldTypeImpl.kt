package com.intellij.dataWrangler.impl.operations

import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.operations.FieldType
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


open class FieldTypeImpl<T : Any, P : Any>(val fieldDesc: DWTypeDesc.DWStructTypeDesc.Field<P, T>) : FieldType<P, T>(fieldDesc.id) {
  val desc: DWTypeDesc<T> get() = fieldDesc.desc

  override fun set(instance: P, value: T) {
    fieldDesc.accessor.set(instance, value)
  }

  override fun get(instance: P): T {
    return fieldDesc.accessor.get(instance)
  }

  fun getPossibleValues(context: DataWranglerContext?): List<T>? {
    return (desc as? DWTypeDesc.DWPrimitiveTypeDesc<T>)?.possibleValues?.getValues(context)
  }

  @Suppress("UNCHECKED_CAST")
  override fun getType(): KClass<T> = desc.targetType!!

  @Suppress("UNCHECKED_CAST")
  inline fun <reified U: Any> tryCast(): FieldTypeImpl<U, P>? =
    if (getType().isSubclassOf(U::class)) this as FieldTypeImpl<U, P> else null
}