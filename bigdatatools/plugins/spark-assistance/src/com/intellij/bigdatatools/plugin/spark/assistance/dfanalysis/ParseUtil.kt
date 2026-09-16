package com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis

import com.intellij.openapi.util.NlsSafe
import com.jetbrains.bigdatatools.common.rfs.client.SchemaInfoPart

object ParseUtil {
  private val STRING_TYPE_MAPPING = hashMapOf(
    Pair("integer", IntegerType),
    Pair("int32", IntegerType),
    Pair("int", IntegerType),
    Pair("int64", LongType),
    Pair("int96", TimestampType),
    Pair("long", LongType),
    Pair("float", FloatType),
    Pair("double", DoubleType),
    Pair("boolean", BooleanType),
    Pair("bool", BooleanType),
    Pair("byte", ByteType),
    Pair("tinyint", ByteType),
    Pair("short", ShortType),
    Pair("smallint", ShortType),
    Pair("string", StringType),
    Pair("binary", StringType), // TODO !
    Pair("varchar", StringType),
    Pair("text", StringType),
    Pair("timestamp", TimestampType),
    Pair("date", DateType),

    Pair("time", TimestampType),
    Pair("datetime", TimestampType),
    Pair("real", DoubleType)
  )

  fun parseMeta(meta: List<SchemaInfoPart>): DfTypeSchema? {
    val parsed = meta.mapNotNull {
      if (it.hasChildren()) return@mapNotNull Pair(it.nameString(), UnknownType)
      val tpe = mapType(it.typeString())
      if (tpe == null) genericParse(it.text) else Pair(it.nameString(), tpe)
    }
    if (parsed.isEmpty()) return null
    return DfTypeSchema(parsed.toMap())
  }

  fun mapType(tpeString: String): DfColumnType? = STRING_TYPE_MAPPING[tpeString.trim().lowercase()]

  fun genericParse(schemaText: String): Pair<String, DfColumnType>? {
    var txt = schemaText.lowercase().removePrefix("required ").removePrefix("optional ").trim()
    if (txt.endsWith(")")) {
      val i = txt.indexOf("(")
      if (i != -1) txt = txt.substring(0, i-1)
    }

    val tpe = if (txt.startsWith("boolean")) BooleanType
    else if (txt.startsWith("int")) IntegerType
    else
      if (txt.startsWith("double")) DoubleType else StringType
    val j = txt.lastIndexOf(' ')
    if (j == -1) return null

    return Pair(txt.substring(j+1), tpe)
  }

  fun schemaToHtml(schema: DfTypeSchema): @NlsSafe String {
    fun aliasesToString(columnName: String): String = schema.aliases[columnName]?.joinToString(prefix = "[", postfix = "]") ?: ""

    val sb = StringBuilder("<html>")

    sb.append("<tr><td><b>").append("name").append("</b></td><td><b>").append("type").append("</b></td></tr>")
    schema.map.forEach {
      sb.append("<tr><td>").append("${it.key}${aliasesToString(it.key)}").append("</td><td>").append(it.value.jsonStructure).append(
        "</td></tr>")
    }
    sb.append("</html>")
    return sb.toString()
  }

  fun schemaToDdlString(schema: DfTypeSchema): String {
    return schema.map.entries.joinToString(", ") { (name, type) ->
      "$name ${type.sqlName}"
    }
  }

  fun parseDdlString(schemaString: String): DfTypeSchema {
    var depth = 0
    val current = StringBuilder()
    val elements = mutableListOf<String>()

    for (c in schemaString) when (c) {
      ',' -> if (depth == 0) {
        elements.add(current.toString())
        current.clear()
      }
      '<' -> depth++
      '>' -> depth--
      else -> if (depth == 0) current.append(c) // ignore structure type for now
    }

    if (depth == 0 && current.isNotEmpty()) elements.add(current.toString())
    return DfTypeSchema(elements.mapNotNull { parseDdlElement(it) }.toMap())
  }

  /*
    Right now we need a very limited set of a particular DDL schema element: <name><space><type>
   */
  private fun parseDdlElement(elementString: String): Pair<String, DfColumnType>? {
    val txt = elementString.trim()
    val i1 = txt.indexOf(char = ' ')
    if (i1 == -1) return null
    var i2 = txt.indexOf(char = ' ', startIndex = i1 + 1)
    if (i2 == -1) i2 = txt.length

    val name = txt.substring(0, i1).removePrefix("`").removeSuffix("`")
    val typeString = txt.substring(i1 + 1, i2).let { // need to support varchar(x) stuff
      if (it.endsWith(")")) {
        val idx = it.indexOf('(')
        if (idx != -1) it.substring(0, idx) else it
      } else it
    }

    val type = DfColumnTypesUtil.DDL_TYPES[typeString.uppercase()] ?: UnknownType

    return Pair(name, type)
  }

}