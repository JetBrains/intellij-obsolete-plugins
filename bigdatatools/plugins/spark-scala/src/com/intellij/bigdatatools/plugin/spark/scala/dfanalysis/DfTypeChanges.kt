package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.UnknownType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfMultipleErrors
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfTypeCheckError
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.getError
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.expr.DfRecursiveExprFolder
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall

/**
 * These two types (DfTypeChangeDescriptor and DfTypeChange) describe changes to a type of one or more columns in a DataFrame.
 * DfTypeChangeDescriptor describes generic type of change that some transformation performs (e.g. withColumn(name, type) adds
 * column `name` with  type `type`), and DfTypeChange describes a particular change, e.g. withColumn("Foo", <StringType>) adds
 * column `Foo` of StringType
 */
sealed class DfTypeChangeDescriptor {
  abstract fun getFoldableTypes(): List<DfFoldedType<*>>
  open fun produceChange(expr: ScExpression): DfTypeChange? =
    if (!associateParameters(expr, getFoldableTypes())) null else constructChange()

  abstract fun constructChange(): DfTypeChange?

  protected fun associateParameters(expr: ScExpression, names: List<DfFoldedType<*>>): Boolean {
    val namesString = HashSet(names.filter { !it.isOptional }.map { it.name })
    val namesType = names.associateBy { it.name }

    for (matched in expr.matchedParameters()) {
      val currentName = matched._2().name()
      namesType[currentName]?.let { tpe ->
        val folded = tpe.fold(matched._1())
        if (folded) namesString.remove(currentName)
      }
    }

    return namesString.isEmpty()
  }
}

abstract class DfSingleTypeChangeMarker : DfTypeChangeDescriptor()

sealed interface DfFoldedType<T> {
  val name: String
  val isOptional: Boolean

  fun fold(expr: ScExpression): Boolean

  fun getFolded(): T

  fun getOuter(): Any? = getFolded()
  fun isSetOuter(): Boolean
  fun setOuter(v: Any)
}

abstract class DfSimpleFoldedTypeBase<T>(override val name: String) : DfFoldedType<T> {
  override val isOptional = false

  protected val folder = DfRecursiveExprFolder()
  protected var c: T? = null

  override fun getFolded(): T = c!!
  override fun isSetOuter(): Boolean = c != null
}

class DfFoldedStringType(name: String) : DfSimpleFoldedTypeBase<String>(name) {
  override fun fold(expr: ScExpression): Boolean {
    c = folder.foldString(expr)
    return c != null
  }

  override fun setOuter(v: Any) {
    (v as? String)?.let { c = v }
  }
}

class DfFoldedColumnType(name: String) : DfSimpleFoldedTypeBase<DfColumnType>(name) {
  override fun fold(expr: ScExpression): Boolean {
    c = folder.foldColumnType(expr) ?: UnknownType
    return true
  }

  override fun setOuter(v: Any) {
    (v as? DfColumnType)?.let { c = v }
  }
}

class DfFoldedRecursiveType(paramName: String) : DfSimpleFoldedTypeBase<DfTypeChange>(paramName) {
  override fun fold(expr: ScExpression): Boolean {
    c = DfTypeByNameUtil.getProvider().findTypeChange(if (expr is ScMethodCall) expr.deepestInvokedExpr() else expr)
    return c != null
  }

  override fun setOuter(v: Any) {
    (v as? DfTypeChange)?.let { c = it }
  }
}

class DfFoldedExtractSchemaType(paramName: String) : DfSimpleFoldedTypeBase<DfTypeContext>(paramName) {
  override fun fold(expr: ScExpression): Boolean {
    c = DfComputingUtil.findContext(expr)
    return c != null
  }

  override fun setOuter(v: Any) {
    (v as? DfTypeContext)?.let { c = it }
  }
}

data class DfFoldedVarargType(val base: DfTypeChangeDescriptor, override val isOptional: Boolean) : DfFoldedType<List<DfTypeChange>> {
  private val result = mutableListOf<DfTypeChange>()
  private val singleType = base.getFoldableTypes().first()
  private val savedValues = mutableListOf<Any>()

  override val name = singleType.name

  override fun fold(expr: ScExpression): Boolean {
    if (singleType.fold(expr)) {
      singleType.getFolded()?.let { savedValues.add(it) }
      base.constructChange()?.let { result.add(it) }
    }
    return true
  }

  override fun getFolded(): List<DfTypeChange> = result

  override fun getOuter(): Any = savedValues

  override fun isSetOuter(): Boolean = singleType.isSetOuter()

  override fun setOuter(v: Any) {
    singleType.setOuter(v)
  }
}

class DfWideContextFoldedType(name: String, override val isOptional: Boolean) : DfSimpleFoldedTypeBase<TypedPair>(name) {
  override fun fold(expr: ScExpression): Boolean {
    folder.foldContextType(expr)?.let { pair -> c = TypedPair(pair.first, pair.second) }
    return c != null
  }

  override fun setOuter(v: Any) {
    // do nothing - shouldn't be aggregating type
  }
}

data class TypedPair(val first: String?, val second: DfColumnType?)

data class DfIntroduceTypeDescriptor(val columnName: String, val columnType: String, val isNew: Boolean = true) : DfTypeChangeDescriptor() {
  private val cname = DfFoldedStringType(columnName)
  private val ctype = DfFoldedColumnType(columnType)
  override fun getFoldableTypes(): List<DfFoldedType<*>> = listOf(cname, ctype)
  override fun constructChange(): DfTypeChange = DfIntroduceType(cname.getFolded(), ctype.getFolded(), isNew, columnName)
}

data class DfFixedTypeDescriptor(val columnName: String,
                                 val columnType: DfColumnType,
                                 val isNew: Boolean = true) : DfSingleTypeChangeMarker() {
  private val cname = DfFoldedStringType(columnName)
  override fun getFoldableTypes(): List<DfFoldedType<*>> = listOf(cname)
  override fun constructChange(): DfTypeChange = DfIntroduceType(cname.getFolded(), columnType, isNew, columnName)
}

data class DfSelectColumnDescriptor(val columnName: String) : DfSingleTypeChangeMarker() {
  private val cname = DfFoldedStringType(columnName)
  override fun getFoldableTypes(): List<DfFoldedType<*>> = listOf(cname)
  override fun constructChange(): DfTypeChange = DfSelectTypeChange(listOf(Pair(cname.getFolded(), columnName)))
}

data class DfRenameColumnDescriptor(val oldColumnName: String, val newColumnName: String) : DfTypeChangeDescriptor() {
  private val oldName = DfFoldedStringType(oldColumnName)
  private val newName = DfFoldedStringType(newColumnName)
  override fun getFoldableTypes(): List<DfFoldedType<*>> = listOf(oldName, newName)
  override fun constructChange(): DfTypeChange = DfRenameColumn(oldName.getFolded(), newName.getFolded(), oldColumnName, newColumnName)
}

data class DfDeleteColumnDescriptor(val columnName: String) : DfSingleTypeChangeMarker() {
  private val cname = DfFoldedStringType(columnName)
  override fun getFoldableTypes(): List<DfFoldedType<*>> = listOf(cname)
  override fun constructChange(): DfTypeChange = DfDeleteColumn(listOf(cname.getFolded()), columnName)
}

data class DfVarargTypeChangeDescriptor(val base: DfSingleTypeChangeMarker, val isOptional: Boolean = false) : DfTypeChangeDescriptor() {
  private val varargTpe = DfFoldedVarargType(base, isOptional)

  override fun getFoldableTypes(): List<DfFoldedType<*>> = listOf(varargTpe)

  override fun constructChange(): DfTypeChange = DfMultipleTypeChanges(DfComputingUtil.handleVarargParams(varargTpe.getFolded()))
}

data class DfMergeSchemaChangeDescriptor(val dfName: String, val checkType: String) : DfTypeChangeDescriptor() {
  private val dfNameType = DfFoldedExtractSchemaType(dfName)
  private val ctype = DfFoldedColumnType(checkType)

  override fun getFoldableTypes(): List<DfFoldedType<*>> = listOf(dfNameType, ctype)

  override fun constructChange(): DfTypeChange = DfMergeTypeChange(dfNameType.getFolded(), ctype.getFolded())
}

data class DfContextTypeChangeDescriptor(val baseName: String,
                                         val transitive: Boolean = true) : DfSingleTypeChangeMarker() { //technically all needed data is in type support
  private val contextType = DfWideContextFoldedType(baseName, false)

  override fun getFoldableTypes(): List<DfFoldedType<*>> = listOf(contextType)

  override fun constructChange(): DfTypeChange? {
    val pair = contextType.getFolded()

    val columnName = pair.first ?: return null // todo
    val columnType = pair.second ?: UnknownType

    return when (columnType) {
      is AliasedColumnType ->
        DfMultipleTypeChanges(
          listOf(
            DfTransitiveTypeChange(DfIntroduceType(columnName, columnType.base, false, baseName), transitive),
            DfTransitiveTypeChange(DfAliasTypeChange(columnName, columnType.getAliases(), baseName, baseName), false)
          )
        )
      else -> DfTransitiveTypeChange(DfIntroduceType(columnName, columnType, false, baseName), transitive)
    }
  }
}

data class DfCompoundTypeChangeDescriptor(val changeDescriptors: List<DfTypeChangeDescriptor>,
                                          val partialAllowed: Boolean = true) : DfTypeChangeDescriptor() {
  override fun getFoldableTypes(): List<DfFoldedType<*>> {
    return changeDescriptors.flatMap { it.getFoldableTypes() }
  }

  override fun constructChange(): DfTypeChange? {
    val changes = changeDescriptors.mapNotNull { it.constructChange() }
    return if (changes.size != changeDescriptors.size && !partialAllowed) null else DfMultipleTypeChanges(changes)
  }
}

data class DfRecursiveTypeChangeDescriptor(val paramName: String) : DfSingleTypeChangeMarker() {
  private val columnDescriptor = DfFoldedRecursiveType(paramName)

  override fun getFoldableTypes(): List<DfFoldedType<*>> = listOf(columnDescriptor)

  override fun constructChange(): DfTypeChange = columnDescriptor.getFolded()
}

data class DfSuppressedSingleTypeChangeDescriptor(val delegate: DfSingleTypeChangeMarker) : DfSingleTypeChangeMarker() {
  override fun getFoldableTypes(): List<DfFoldedType<*>> = delegate.getFoldableTypes()

  override fun constructChange(): DfTypeChange? = delegate.constructChange()?.let { DfSuppressedTypeChange(it) }
}

data class DfSuppressedTypeChangeDescriptor(val delegate: DfTypeChangeDescriptor) : DfTypeChangeDescriptor() {
  override fun getFoldableTypes(): List<DfFoldedType<*>> = delegate.getFoldableTypes()

  override fun constructChange(): DfTypeChange? = delegate.constructChange()?.let { DfSuppressedTypeChange(it) }
}

data class DfAggregatingChangeDescriptor(
  val base: DfTypeChangeDescriptor,
  val descriptors: List<DfSingleTypeChangeMarker>,
  val transform: (String, Any) -> Any
) : DfTypeChangeDescriptor() {
  private val computed = HashMap<String, Any>()

  override fun getFoldableTypes(): List<DfFoldedType<*>> = descriptors.flatMap { it.getFoldableTypes() }

  override fun produceChange(expr: ScExpression): DfTypeChange? {
    if (!associateParameters(expr, base.getFoldableTypes())) return null
    val baseChange = base.constructChange() ?: return null
    val allChanges = mutableListOf(baseChange)

    base.getFoldableTypes().forEach { foldableType ->
      foldableType.getOuter()?.let { computed[foldableType.name] = it }
    }

    for (desc in descriptors) {
      val singleType = desc.getFoldableTypes().firstOrNull() ?: continue

      fun produceChange(tpeAnyVal: Any) {
        singleType.setOuter(transform(singleType.name, tpeAnyVal))
        if (check(desc)) desc.constructChange()?.let { allChanges.add(it) }
      }

      computed[singleType.name]?.let { computedAny ->
        when (computedAny) {
          is List<*> -> for (anyVal in computedAny) anyVal?.let { produceChange(it) }
          else -> produceChange(computedAny)
        }
      }
    }

    return DfMultipleTypeChanges(allChanges)
  }

  override fun constructChange(): DfTypeChange? {
    val changes = descriptors.mapNotNull { descriptor ->
      if (check(descriptor)) descriptor.constructChange() else null
    }

    val baseChange = base.constructChange() ?: return null

    return if (changes.isEmpty()) baseChange else DfMultipleTypeChanges(mutableListOf(baseChange).also { it.addAll(changes) })
  }

  private fun check(descriptor: DfTypeChangeDescriptor): Boolean = descriptor.getFoldableTypes().all { it.isSetOuter() }
}

sealed interface DfTypeChange {
  fun applyChanges(context: DfTypeContext, placeDesc: String): DfTypeCheckError?
}

interface DfMergeableTypeChange : DfTypeChange {
  fun merge(other: DfMergeableTypeChange): DfMergeableTypeChange
}

data class DfTransitiveTypeChange(val delegate: DfTypeChange, val transitive: Boolean) : DfTypeChange by delegate {
  override fun applyChanges(context: DfTypeContext, placeDesc: String): DfTypeCheckError? {
    val error = delegate.applyChanges(context, placeDesc)
    return if (transitive) null else error
  }
}

data class DfIntroduceType(val columnName: String,
                           val columnType: DfColumnType,
                           val isNew: Boolean,
                           val placeName: String?) : DfTypeChange {
  override fun applyChanges(context: DfTypeContext, placeDesc: String): DfTypeCheckError? =
    context.add(columnName, columnType, placeDesc, placeName, isNew)
}

data class DfRenameColumn(val oldColumnName: String,
                          val newColumnName: String,
                          val oldPlace: String?,
                          val newPlace: String?) : DfTypeChange {
  override fun applyChanges(context: DfTypeContext, placeDesc: String): DfTypeCheckError? =
    context.rename(oldColumnName, newColumnName, "Renamed by $placeDesc", oldPlace, newPlace)
}

data class DfAliasTypeChange(val baseName: String,
                             val aliases: Set<String>,
                             val namePlace: String?,
                             val aliasPlace: String?) : DfTypeChange {
  override fun applyChanges(context: DfTypeContext, placeDesc: String): DfTypeCheckError? {
    val errors = mutableSetOf<DfTypeCheckError>()
    aliases.forEach { context.addAliases(baseName, it, placeDesc, namePlace, aliasPlace)?.let { error -> errors.add(error) } }
    return errors.getError()
  }
}

data class DfDeleteColumn(val columnNames: List<String>, val namePlace: String?) : DfMergeableTypeChange {
  override fun applyChanges(context: DfTypeContext, placeDesc: String): DfTypeCheckError? =
    context.remove(placeDesc, namePlace, columnNames)

  override fun merge(other: DfMergeableTypeChange): DfMergeableTypeChange {
    return when (other) {
      is DfDeleteColumn -> DfDeleteColumn(columnNames.plus(other.columnNames), namePlace)
      else -> this
    }
  }
}

data class DfSuppressedTypeChange(val delegate: DfTypeChange) : DfTypeChange {
  override fun applyChanges(context: DfTypeContext, placeDesc: String): DfTypeCheckError? {
    val error = delegate.applyChanges(context, placeDesc) ?: return null
    if (!error.suppressable()) return error

    if (error is DfMultipleErrors) {
      val errors = error.errors.filter { !it.suppressable() }
      return errors.getError()
    }

    return null
  }
}

//Pair(column name, parameter name place)
data class DfSelectTypeChange(val columnNames: List<Pair<String, String>>) : DfMergeableTypeChange {
  override fun applyChanges(context: DfTypeContext, placeDesc: String): DfTypeCheckError? =
    context.select(placeDesc, columnNames)

  override fun merge(other: DfMergeableTypeChange): DfMergeableTypeChange {
    return when (other) {
      is DfSelectTypeChange -> DfSelectTypeChange(columnNames.plus(other.columnNames))
      else -> this
    }
  }
}

data class DfMergeTypeChange(val df: DfTypeContext, val checkType: DfColumnType?) : DfTypeChange {
  override fun applyChanges(context: DfTypeContext, placeDesc: String): DfTypeCheckError? {
    return context.merge(df, checkType)
  }
}

data class DfMultipleTypeChanges(val changes: List<DfTypeChange>) : DfTypeChange {
  override fun applyChanges(context: DfTypeContext, placeDesc: String): DfTypeCheckError? {
    val errors = mergeAllPossible(changes).mapNotNull { it.applyChanges(context, placeDesc) }
    return errors.getError()
  }

  // todo right now we have only one possible mergeable type change, but we might need multiple mergeable changes support later
  private fun mergeAllPossible(changes: List<DfTypeChange>): List<DfTypeChange> {
    var acc: DfMergeableTypeChange? = null
    val result = mutableListOf<DfTypeChange>()

    fun mergeInner(ci: List<DfTypeChange>) {
      for (change in ci) when (change) {
        is DfMergeableTypeChange -> if (acc != null) acc = acc!!.merge(change) else acc = change
        is DfMultipleTypeChanges -> mergeInner(change.changes)
        else -> result.add(change)
      }
    }

    mergeInner(changes)
    if (acc != null) result.add(acc!!)

    return result
  }
}