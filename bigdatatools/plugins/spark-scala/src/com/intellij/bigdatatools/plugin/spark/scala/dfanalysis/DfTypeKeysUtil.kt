package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfTypeCheckError
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange

object DfTypeKeysUtil {
  private val ERRORS_KEY = Key.create<MutableSet<DfTypeCheckError>>("DF_ERRORS_KEY")
  private val RANGE_KEY = Key.create<TextRange>("DF_SOURCE_EXPR_RANGE_KEY")

  val ALIASES_KEY = Key.create<MutableSet<String>>("DF_ALIASES_KEY")
  val CAST_KEY = Key.create<Boolean>("DF_CAST_SEEN_KEY")

  fun getTypeErrors(tpe: DfColumnType?): Set<DfTypeCheckError>? =
    if (tpe is TypeWithAttachments) tpe.getUserData(ERRORS_KEY) else null

  fun addTypeError(tpe: DfColumnType, error: DfTypeCheckError): DfColumnType = attachToSet(tpe, ERRORS_KEY, error)

  fun <T> attach(tpe: DfColumnType, key: Key<T>, t: T): DfColumnType = convert(tpe).also { it.putUserData(key, t) }

  fun <T> attachToSet(tpe: DfColumnType, key: Key<MutableSet<T>>, t: T): DfColumnType = convert(tpe).also { it.putUserDataMap(key, t) }

  fun attachRange(tpe: DfColumnType, range: TextRange): DfColumnType = attach(tpe, RANGE_KEY, range)

  fun getRange(tpe: DfColumnType): TextRange? = if (tpe !is TypeWithAttachments) null else tpe.getUserData(RANGE_KEY)

  fun transfer(target: DfColumnType, vararg sources: DfColumnType): DfColumnType {
    val result = if (target is TypeWithAttachments) target else TypeWithAttachments(target)
    for (source in sources) if (source is TypeWithAttachments) source.copyUserDataTo(result)
    return result
  }

  private fun convert(tpe: DfColumnType): TypeWithAttachments = when (tpe) {
    is TypeWithAttachments -> tpe
    else -> TypeWithAttachments(tpe)
  }
}