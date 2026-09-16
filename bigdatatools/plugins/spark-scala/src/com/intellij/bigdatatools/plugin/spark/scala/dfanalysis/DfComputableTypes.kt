package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.BooleanType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.NumericType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.TimestampType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.UnknownType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfTypeCheckError
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfTypeOpWeakError
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import com.intellij.openapi.util.TextRange

object DfComputableTypes {
  object IdAnyTypeCreator : ComputableTypeCreator<ComputableColumnType1>("") {
    override fun createType(range: TextRange?): ComputableColumnType1 = IdAnyType

    object IdAnyType : ComputableColumnType1(UnknownType) {
      override fun computeImpl(t: DfColumnType): DfColumnType = t
    }
  }

  class IdNumericTypeCreator(opString: String) : ComputableTypeCreator<ComputableColumnType1>(opString) {
    override fun createType(range: TextRange?): ComputableColumnType1 = IdNumericType(opString)

    class IdNumericType(private val opString: String) : ComputableColumnType1(NumericType) {
      override fun computeImpl(t: DfColumnType): DfColumnType =
        processType(t, NumericType, opString, false)?.let { DfTypeKeysUtil.addTypeError(t, it) } ?: t
    }
  }

  class IdBooleanTypeCreator(opString: String) : ComputableTypeCreator<ComputableColumnType1>(opString) {
    override fun createType(range: TextRange?): ComputableColumnType1 = IdBooleanType(opString)

    class IdBooleanType(private val opString: String) : ComputableColumnType1(BooleanType) {
      override fun computeImpl(t: DfColumnType): DfColumnType =
        processType(t, BooleanType, opString, false)?.let { DfTypeKeysUtil.addTypeError(t, it) } ?: t
    }
  }

  object IdTimestampTypeCreator : ComputableTypeCreator<ComputableColumnType1>("") {
    override fun createType(range: TextRange?): ComputableColumnType1 = IdTimestampType

    object IdTimestampType : ComputableColumnType1(TimestampType) {
      override fun computeImpl(t: DfColumnType): DfColumnType = TimestampType // todo if t converts to timestamp
    }
  }

  object IdDropRightTimestampCreator : ComputableTypeCreator<ComputableColumnType2>("") {
    override fun createType(range: TextRange?): ComputableColumnType2 = IdDropRightTimestamp

    object IdDropRightTimestamp : ComputableColumnType2(TimestampType) {
      override fun computeImpl(left: DfColumnType, right: DfColumnType): DfColumnType = TimestampType // todo check if left converts to timestamp
    }
  }

  class UnionNumericTypeCreator(opString: String) : ComputableTypeCreator<ComputableColumnType2>(opString) {
    override fun createType(range: TextRange?): ComputableColumnType2 = UnionNumericType(opString, range)

    class UnionNumericType(private val opString: String, private val range: TextRange?) : ComputableColumnType2(NumericType) {
      override fun computeImpl(left: DfColumnType, right: DfColumnType): DfColumnType {
        val unionLeftRight = left.union(right)
        val unionTpe = if (DfComputingUtil.isEmpty(unionLeftRight)) baseType else unionLeftRight!!
        val midType = processType(left, baseType, opString, true, range)?.let { DfTypeKeysUtil.addTypeError(unionTpe, it) } ?: unionTpe

        return processType(right, baseType, opString, true, range)?.let { DfTypeKeysUtil.addTypeError(midType, it) } ?: midType
      }
    }
  }

  class UnionBooleanTypeCreator(opString: String) : ComputableTypeCreator<ComputableColumnType2>(opString) {
    override fun createType(range: TextRange?): ComputableColumnType2 = UnionBooleanType(opString, range)

    class UnionBooleanType(private val opString: String, private val range: TextRange?) : ComputableColumnType2(BooleanType) {
      override fun computeImpl(left: DfColumnType, right: DfColumnType): DfColumnType {
        if (DfComputingUtil.isEmpty(left.union(right)) && DfComputingUtil.isEmpty(right.union(left))) {
          val finalRange = range ?: DfTypeKeysUtil.getRange(left) ?: DfTypeKeysUtil.getRange(right) ?: return BooleanType
          val error = DfTypeOpWeakError(left, right, opString, finalRange)
          return DfTypeKeysUtil.addTypeError(BooleanType, error)
        }

        return BooleanType
      }
    }
  }

  fun processType(tpe: DfColumnType, checkedType: DfColumnType, opString: String, binary: Boolean, outerRange: TextRange? = null): DfTypeCheckError? {
    val range = DfTypeKeysUtil.getRange(tpe) ?: outerRange ?: return null
    return if (tpe.isCompatible(checkedType)) null else DfTypeOpWeakError(tpe, if (binary) checkedType else null, opString, range)
  }
}

abstract class ComputableTypeCreator<T : DfColumnType>(val opString: String) {
  abstract fun createType(range: TextRange?): T
}