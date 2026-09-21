package com.jetbrains.bigdatatools.common.rfs.localcache.metainfo

import com.jetbrains.bigdatatools.common.rfs.client.FileMetaInfoProvider
import com.jetbrains.bigdatatools.common.rfs.client.SchemaInfoPart
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import org.apache.avro.Schema
import org.apache.avro.Schema.Type.ARRAY
import org.apache.avro.Schema.Type.ENUM
import org.apache.avro.Schema.Type.FIXED
import org.apache.avro.Schema.Type.MAP
import org.apache.avro.Schema.Type.NULL
import org.apache.avro.Schema.Type.RECORD
import org.apache.avro.Schema.Type.UNION
import org.apache.avro.file.DataFileStream
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import java.util.Locale

class AvroMetaInfoProvider : FileMetaInfoProvider() {
  override fun hasMetaInfo(rfsPath: RfsPath) = rfsPath.name.endsWith(".avro")

  override suspend fun getMetaInfo(fileInfo: FileInfo): List<AvroInfoPart> {
    val fileInfoStream = fileInfo.readStream(0, null).resultOrThrow()
    return DataFileStream<GenericRecord>(fileInfoStream.buffered(), GenericDatumReader()).use { stream ->
      val fields = stream.schema.fields
      fields.map { it.schema().asInfoPart(it.name()) }
    }
  }

  private fun Schema.asInfoPart(name: String? = null): AvroInfoPart = when (type) {
    UNION ->
      if (types.size == 2 && types.any { it.type == NULL })
        types
          .firstOrNull { it.type != NULL }
          ?.asInfoPart(name)
          ?.optional() ?: AvroInfoPart(name, "NULL")
      else AvroInfoPart(name, "Union", types.map { it.asInfoPart() })
    MAP -> AvroInfoPart(name, "Map", listOf(valueType.asInfoPart()))
    ARRAY -> AvroInfoPart(name, "Array", listOf(elementType.asInfoPart()))
    RECORD -> AvroInfoPart(name, "Record", fields.map { it.schema().asInfoPart(it.name()) })
    ENUM -> AvroInfoPart(name, enumSymbols.joinToString("|", prefix = "Enum[", postfix = "]"))
    FIXED -> AvroInfoPart(name, "Fixed($fixedSize)")
    else -> AvroInfoPart(name,
      type.getName().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
  }

  data class AvroInfoPart(
    private val name: String?,
    val type: String,
    override val children: List<SchemaInfoPart> = listOf(),
    val isOptional: Boolean = false
  ) : SchemaInfoPart {
    override val text get() =
      (if (name.isNullOrBlank()) "" else "$name: ") + (if (isOptional) "optional " else "") + type
    override val icon get() = RfsIcons.META_PART_ICON
    override val onClick get() = null
    override fun hasChildren() = children.isNotEmpty()
    fun optional() = copy(isOptional = true)

    override fun typeString(): String = type

    override fun nameString(): String = name ?: ""

    fun toHtml(): String {
      val childrenFormatted = if (children.isEmpty())
        ""
      else {
        val childrenString = children.joinToString("")
        "<ul>$childrenString</ul>"
      }
      return """<li>$text$childrenFormatted</li>"""
    }

  }
}

