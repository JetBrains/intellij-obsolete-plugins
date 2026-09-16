package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfColumnType
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSchema
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.NamedTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.UnknownType
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfMultipleErrors
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfTypeCheckAliasAlreadyExists
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfTypeCheckColumnAlreadyExists
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfTypeCheckConflict
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfTypeCheckError
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.DfTypeCheckMiss
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.error.getError
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfComputingUtil

class DfTypeContext(val isPartial: Boolean = false) {
  private val state = LinkedHashMap<String, DfColumnDescriptor>() // Column name -> (Column Type, Last update description)

  private val aliases = HashMap<String, HashSet<String>>()
  private val aliasesReverse = HashMap<String, String>()

  private var name = ""

  constructor(source: DfTypeSource) : this(source.isPartial) {
    start(source.schema(), source.description)
    if (source is NamedTypeSource) name = source.name // todo probably we can save the whole datasource to preserve its description and original schema
  }

  private fun start(schema: DfTypeSchema, sourceDesc: String) {
    for (entry in schema.map) state[entry.key] = DfColumnDescriptor.create(entry.value, sourceDesc, state, null)
  }

  fun add(name: String, tpe: DfColumnType, placeDesc: String, placeName: String?, isNew: Boolean): DfTypeCheckError? {
    val name_ = aliasesReverse[name] ?: name
    val present = state[name_]
    val errors = mutableListOf<DfTypeCheckError>()

    if (present == null && !isPartial && !isNew) return DfTypeCheckMiss(placeDesc, name_, placeName)
    if (present == null || !DfComputingUtil.isEmpty(tpe)) state[name_] = DfColumnDescriptor.create(tpe, placeDesc, state, errors)

    if (present != null && !tpe.isCompatible(present.columnType))
      errors.add(DfTypeCheckConflict(name_, present.lastUpdateDesc, present.columnType, placeDesc, tpe, placeName))

    return errors.getError()
  }

  fun rename(oldName: String, newName: String, placeDesc: String, oldPlaceName: String?, newPlaceName: String?): DfTypeCheckError? {
    if (!state.containsKey(oldName) && !isPartial) {
      val missError = DfTypeCheckMiss(placeDesc, oldName, oldPlaceName)
      return checkNameAlreadyExists(newName, placeDesc, newPlaceName)?.let { DfMultipleErrors(listOf(missError, it)) } ?: missError
    }

    val errors = mutableListOf<DfTypeCheckError>()
    checkNameAlreadyExists(newName, placeDesc, newPlaceName)?.let { errors.add(it) }

    val old = state.remove(oldName)
    state[newName] = DfColumnDescriptor.create(old?.columnType ?: UnknownType, placeDesc, state, errors)

    aliases.remove(oldName)?.let { oldAliases ->
      for (alias in oldAliases) aliasesReverse[alias] = newName
      aliases[newName] = oldAliases
    }

    return errors.getError()
  }

  fun addAliases(base: String, baseAlias: String, placeDesc: String, namePlace: String?, aliasPlace: String?): DfTypeCheckError? {
    val errors = mutableListOf<DfTypeCheckError>()

    if (!state.containsKey(base)) {
      if (!isPartial) return DfTypeCheckMiss(placeDesc, base, namePlace)
      state[base] = DfColumnDescriptor.create(UnknownType, placeDesc, state, errors)
    }

    checkNameAlreadyExists(baseAlias, placeDesc, aliasPlace)?.let { errors.add(it) }

    aliases.getOrPut(base) { HashSet() }.add(baseAlias)
    aliasesReverse[baseAlias] = base

    return errors.getError()
  }

  //Pair(column name, corresponding parameter name)
  fun select(placeDesc: String, names: List<Pair<String, String?>>): DfTypeCheckError? {
    val ns = names.map { it.first }.toSet()
    if (ns.contains("*")) return null

    val errors = mutableSetOf<DfTypeCheckError>()

    val iter = state.iterator()
    while (iter.hasNext()) {
      val columnName = iter.next().key

      if (!ns.contains(columnName)) {
        iter.remove()
        removeAliases(columnName)
      }
    }

    val parameterGroups = names.groupBy { it.second }

    for (group in parameterGroups)
      for (i in group.value.indices) {
        val (columnName, namePlace) = group.value[i]
        if (!state.containsKey(columnName)) {
          if (!isPartial)
            errors.add(DfTypeCheckMiss(placeDesc, columnName, namePlace, i))
          else
            state[columnName] = DfColumnDescriptor.create(UnknownType, placeDesc, state, errors)
        }
      }

    return errors.getError()
  }

  fun getColumnType(name: String): DfColumnType? = (aliasesReverse[name]?.let { state[it] } ?: state[name])?.columnType

  fun remove(placeDesc: String, namePlace: String?, names: List<String>): DfTypeCheckError? {
    val errors = mutableListOf<DfTypeCheckError>()

    for (i in names.indices) {
      val name = names[i]
      if (state.remove(name) == null) errors.add(DfTypeCheckMiss(placeDesc, name, namePlace, i))
      removeAliases(name)
    }

    return if (isPartial) null else errors.getError()
  }

  fun merge(other: DfTypeContext, checkCondition: DfColumnType? = null): DfTypeCheckError? {
    for ((k, v) in other.state) state[k] = v
    return DfTypeKeysUtil.getTypeErrors(checkCondition)?.toList()?.getError()
  }

  fun toSchema(): DfTypeSchema {
    val pairs = state.map {
      Pair(it.key, it.value.columnType)
    }

    return DfTypeSchema(DfComputingUtil.linkedHashMapOf(pairs), aliases.mapValues { HashSet(it.value) })
  }

  fun isEmpty(): Boolean = state.isEmpty()

  fun getSourceName(): String = name

  private fun removeAliases(columnName: String) {
    aliases.remove(columnName)?.forEach { aliasesReverse.remove(it) }
  }

  private fun checkNameAlreadyExists(name: String, placeDesc: String, placeName: String?): DfTypeCheckError? =
    if (state.containsKey(name))
      DfTypeCheckColumnAlreadyExists(placeDesc, name, placeName)
    else if (aliasesReverse.containsKey(name))
      DfTypeCheckAliasAlreadyExists(placeDesc, name, placeName)
    else null
}