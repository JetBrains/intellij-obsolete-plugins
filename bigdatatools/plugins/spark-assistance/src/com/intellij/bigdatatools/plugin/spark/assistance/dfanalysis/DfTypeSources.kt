package com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis

interface DfTypeSource {
  fun schema(): DfTypeSchema

  val description: String

  val isPartial: Boolean
}

open class SimpleTypeSource(val s: DfTypeSchema, override val description: String) : DfTypeSource {
  override fun schema(): DfTypeSchema = s

  override val isPartial: Boolean = false
}

open class NamedTypeSource(schema: DfTypeSchema, description: String, val name: String) : SimpleTypeSource(schema, description)
class EmptyTypeSource(description: String, name: String) : NamedTypeSource(Util.EMPTY_SCHEMA, description, name) {
  object Util {
    val EMPTY_SCHEMA = DfTypeSchema(LinkedHashMap())
  }

  override val isPartial: Boolean = true
}