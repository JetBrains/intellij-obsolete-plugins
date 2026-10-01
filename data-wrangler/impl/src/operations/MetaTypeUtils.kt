package com.intellij.dataWrangler.impl.operations

import com.intellij.dataWrangler.operations.FieldType
import com.intellij.dataWrangler.operations.MetaStruct
import kotlin.reflect.KClass


fun <P : Any> toMetaStructure(kClass: KClass<P>): MetaStruct<P> {
  require(kClass.java.constructors.any { it.parameters.isEmpty() })
  return toMetaStructure(kClass.toString(), kClass, DWTypes.create(kClass) as DWTypeDesc.DWStructTypeDesc<P>)
}

fun <P : Any> toMetaStructure(
  name: String,
  kClass: KClass<P>,
  desc: DWTypeDesc.DWStructTypeDesc<P>,
): MetaStruct<P> {
  // TODO: construct id from class path / class name or something else
  // TODO: What if there's exactly same field in the same data class
  val fields = desc.fields.asSequence().map { createFieldType(it) }
  return MetaStructImpl(name, fields.associateBy { "$kClass#${it.id}" }, desc, kClass)
}

private fun <P : Any, T: Any> createFieldType(
  field: DWTypeDesc.DWStructTypeDesc.Field<P, T>,
): FieldType<P, T> {
  return FieldTypeImpl(field)
}
