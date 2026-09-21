package com.jetbrains.bigdatatools.common.rfs.localcache.metainfo

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.bigdatatools.common.rfs.client.FileMetaInfoProvider
import com.jetbrains.bigdatatools.common.rfs.client.SchemaInfoPart
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.orc.JetbrainsOrcReader
import com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.orc.ORCReadException
import org.apache.orc.OrcUtils
import org.apache.orc.TypeDescription
import org.apache.orc.TypeDescription.Category.LIST
import org.apache.orc.TypeDescription.Category.MAP
import org.apache.orc.TypeDescription.Category.STRUCT
import org.apache.orc.TypeDescription.Category.UNION
import javax.swing.Icon

class ORCMetaInfoProvider : FileMetaInfoProvider() {
  override fun hasMetaInfo(rfsPath: RfsPath) = rfsPath.name.endsWith(".orc")

  override suspend fun getMetaInfo(fileInfo: FileInfo): List<ORCInfoPart> = JetbrainsOrcReader.withInputStream(fileInfo) { input ->
    try {
      if (input == null) return emptyList()
      val fileLength = fileInfo.length
      val (postscript, postscriptLength) = JetbrainsOrcReader.readPostScript(fileLength, input)
      val footer = JetbrainsOrcReader.readFooter(postscript, postscriptLength, fileLength, input)!!
      val rootType = OrcUtils.convertTypeFromProtobuf(footer.typesList, 0)
      rootType.children.mapIndexed { i, it ->
        it.asInfoPart(rootType.fieldNames[i])
      }
    }
    catch (e: ORCReadException) {
      logger.info(e)
      emptyList()
    }
  }

  private fun TypeDescription.asInfoPart(name: String? = null): ORCInfoPart = when (category) {
    LIST -> ORCInfoPart(name, category.name, listOf(children[0].asInfoPart()))
    UNION -> ORCInfoPart(name, category.name, children.map { type -> type.asInfoPart() })
    MAP -> ORCInfoPart(
      name,
      category.name,
      listOf(children[0].asInfoPart("key"), children[0].asInfoPart("value"))
    )
    STRUCT -> ORCInfoPart(
      name,
      category.name,
      children.mapIndexed { i, type -> type.asInfoPart(fieldNames[i]) }
    )
    else -> ORCInfoPart(name, category.name)
  }

  data class ORCInfoPart(val name: String?, val type: String, override val children: List<ORCInfoPart> = emptyList()) : SchemaInfoPart {
    override val text: String
      get() = "${if (name.isNullOrBlank()) "" else "$name: "}$type"
    override val icon: Icon
      get() = RfsIcons.META_PART_ICON
    override val onClick: (() -> Unit)?
      get() = null

    override fun typeString(): String = type

    override fun nameString(): String = name ?: ""

    override fun hasChildren() = children.isNotEmpty()
    fun toHtml(): String {
      val children = if (children.isEmpty()) "" else "<ol>${children.joinToString("")}</ol>"
      return "<li>$text\n$children\n</li>"
    }
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }
}
