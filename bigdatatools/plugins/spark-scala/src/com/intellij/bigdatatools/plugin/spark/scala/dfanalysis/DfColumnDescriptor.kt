package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfTypeCheckError

class DfColumnDescriptor private constructor(val columnType: DfColumnType, val lastUpdateDesc: String) {
  companion object {
    fun create(columnType: DfColumnType,
               lastUpdateDesc: String,
               state: Map<String, DfColumnDescriptor>,
               errors: Collection<DfTypeCheckError>?): DfColumnDescriptor {
      val actualType = when (columnType) {
        is TypeWithAttachments -> {
          val allErrors = (errors?.toList() ?: emptyList()) + (DfTypeKeysUtil.getTypeErrors(columnType)?.toList() ?: emptyList())
          return create(columnType.base, lastUpdateDesc, state, allErrors)
        }
        is OuterComputableType ->
          return create(columnType.compute(state), lastUpdateDesc, state, errors)
        else -> columnType
      }

      return DfColumnDescriptor(actualType, lastUpdateDesc)
    }
  }
}