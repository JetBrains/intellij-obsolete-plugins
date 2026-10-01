package com.intellij.dataWrangler.operations

import com.intellij.openapi.util.NlsSafe
import kotlin.reflect.KClass

open class MetaType<T : Any>(val id: @NlsSafe String)
abstract class FieldType<P: Any, T : Any>(id: @NlsSafe String): MetaType<T>(id) {
  abstract fun set(instance: P, value: T)
  abstract fun get(instance: P): T
  abstract fun getType(): KClass<T>
}

interface MetaStruct<P : Any> {
  val id: String
  val dataClassType: KClass<P>
  fun getFields(): Map<String, FieldType<P, *>>
  fun getNewInstance(): P
}