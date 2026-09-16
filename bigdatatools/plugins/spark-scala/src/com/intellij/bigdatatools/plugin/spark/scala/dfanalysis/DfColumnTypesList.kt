package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfAttachable
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfSyntheticColumnType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.UnknownType

open class TypeWithAttachments(val base: DfColumnType) : DfAttachable(), DfColumnType by base {
  override fun union(other: DfColumnType?): DfColumnType? =
    when (other) {
      is TypeWithAttachments ->
        TypeWithAttachments(base.union(other.base) ?: UnknownType).also {
          copyUserDataTo(it)
          other.copyUserDataTo(it)
        }
      else ->
        TypeWithAttachments(base.union(other) ?: UnknownType).also { copyUserDataTo(it) }
    }

  override fun equals(other: Any?): Boolean {
    return other is TypeWithAttachments && other.base == this.base && other.getUserData(DfTypeKeysUtil.ALIASES_KEY) == this.getUserData(DfTypeKeysUtil.ALIASES_KEY)
  }

  override fun hashCode(): Int = base.hashCode()
}

class AliasedColumnType(base: DfColumnType) : TypeWithAttachments(base) {
  fun getAliases(): Set<String> = getUserData(DfTypeKeysUtil.ALIASES_KEY) ?: emptySet()

  override fun equals(other: Any?): Boolean =
    other is AliasedColumnType && other.base == base && other.getAliases() == getAliases()

  override fun hashCode(): Int = base.hashCode() // attachments are mutable

  companion object {
    fun create(base: DfColumnType, aliases: Collection<String>): AliasedColumnType {
      return when (base) {
        is AliasedColumnType -> {
          aliases.forEach { base.putUserDataMap(DfTypeKeysUtil.ALIASES_KEY, it) }
          base
        }
        is TypeWithAttachments ->
          AliasedColumnType(base.base).also {
            base.copyUserDataTo(it)
            it.putUserData(DfTypeKeysUtil.ALIASES_KEY, aliases.toMutableSet())
          }
        else -> AliasedColumnType(base).also { it.putUserData(DfTypeKeysUtil.ALIASES_KEY, HashSet(aliases)) }
      }
    }
  }
}

abstract class ComputableColumnType1(val baseType: DfColumnType) : DfColumnType by baseType {
  fun compute(t: DfColumnType?): DfColumnType = t?.let { computeImpl(it) }?.let { DfTypeKeysUtil.transfer(it, baseType) } ?: baseType

  protected abstract fun computeImpl(t: DfColumnType): DfColumnType?
}

abstract class ComputableColumnType2(val baseType: DfColumnType) : DfColumnType by baseType {
  fun compute(left: DfColumnType?, right: DfColumnType?): DfColumnType =
    if (left == null || right == null) baseType else computeImpl(left, right)?.let { DfTypeKeysUtil.transfer(it, left, right) } ?: baseType

  protected abstract fun computeImpl(left: DfColumnType, right: DfColumnType): DfColumnType?
}

interface OuterComputableType : DfSyntheticColumnType {
  fun compute(state: Map<String, DfColumnDescriptor>): DfColumnType
}

abstract class OuterComputableType1(private val argColumnName: String, baseType: DfColumnType) : ComputableColumnType1(baseType), OuterComputableType {
  override fun compute(state: Map<String, DfColumnDescriptor>): DfColumnType = compute(state[argColumnName]?.columnType)

  override fun union(other: DfColumnType?): DfColumnType? {
    return if (other == null) super.union(null) else UnionComputableType(this, other, baseType)
  }
}

abstract class OuterComputableType2(
  private val columnName1: String,
  private val columnName2: String,
  baseType: DfColumnType
) : ComputableColumnType2(baseType), OuterComputableType {
  override fun compute(state: Map<String, DfColumnDescriptor>): DfColumnType = compute(state[columnName1]?.columnType, state[columnName2]?.columnType)

  override fun union(other: DfColumnType?): DfColumnType? {
    return if (other == null) super.union(null) else UnionComputableType(this, other, baseType)
  }
}

class UnionComputableType(
  private val tpe1: DfColumnType,
  private val tpe2: DfColumnType,
  base: DfColumnType
) : ComputableColumnType2(base), OuterComputableType {
  override fun computeImpl(left: DfColumnType, right: DfColumnType): DfColumnType? = left.union(right)

  override fun compute(state: Map<String, DfColumnDescriptor>): DfColumnType {
    val t1 = if (tpe1 is OuterComputableType) tpe1.compute(state) else tpe1
    val t2 = if (tpe2 is OuterComputableType) tpe2.compute(state) else tpe2

    return computeImpl(t1, t2) ?: baseType
  }
}