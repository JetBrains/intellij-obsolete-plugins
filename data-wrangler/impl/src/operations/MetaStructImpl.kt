package com.intellij.dataWrangler.impl.operations

import com.intellij.dataWrangler.impl.operations.DWTypeDesc.DWStructTypeDesc
import com.intellij.dataWrangler.operations.FieldType
import com.intellij.dataWrangler.operations.MetaStruct
import kotlin.reflect.KClass

class MetaStructImpl<P : Any>(override val id: String, fields: Map<String, FieldType<P, *>>, val desc: DWStructTypeDesc<P>, override val dataClassType: KClass<P>): MetaStruct<P> {
  private val fields = LinkedHashMap(fields)
  override fun getFields(): Map<String, FieldType<P, *>> = fields
  override fun getNewInstance(): P = dataClassType.java.getConstructor().newInstance()

  companion object {
    inline fun <reified T : Any> create(id: String, desc: DWStructTypeDesc<T>, fields: Map<String, FieldType<T, *>>): MetaStruct<T> {
      val metaStruct = MetaStructImpl(id, fields, desc, T::class)
      return metaStruct
    }
  }
}