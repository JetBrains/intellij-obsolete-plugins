package com.jetbrains.bigdatatools.common.rfs.localcache.analyzing.parquet

import org.apache.parquet.schema.GroupType
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.Type
import java.io.Serializable
import java.util.LinkedList
import java.util.Queue

/**
 * User: Dmitry.Naydanov
 * Date: 2018-11-21.
 */
class SchemaTree(schema: MessageType) {
  private val rootNode = SchemaNodeData(null)

  private val nodes = arrayListOf(rootNode)
  private var typedNodes = emptyList<SchemaNodeData>()


  init {
    var currentParent = rootNode

    currentParent.chFrom = 1
    val queue: Queue<Type> = LinkedList(schema.fields)
    currentParent.chTo = queue.size + 1

    while (queue.isNotEmpty()) {
      val tpe = queue.poll()

      currentParent = SchemaNodeData(tpe, chFrom = nodes.size + queue.size + 1)
      val children = when (tpe) {
        is GroupType -> tpe.fields
        else -> emptyList()
      }

      queue.addAll(children)
      if (children.isNotEmpty()) currentParent.chTo = currentParent.chFrom + children.size
      nodes.add(currentParent)
    }

    typedNodes = nodes.filter { it.tpe?.originalType != null }
  }

  inner class SchemaNodeData(val tpe: Type?, internal var chFrom: Int = 0, internal var chTo: Int = chFrom)

  fun getByIndex(i: Int): SchemaNodeData? = if (i < 0 || i >= nodes.size - 1) null else nodes[i + 1]

  fun getTypedByIndex(i: Int): SchemaNodeData? = if (i < 0 || i >= typedNodes.size) null else typedNodes[i]

  fun makeSerializable(): SerializableSchemaTree =
    SerializableSchemaTree(nodes)
}

class SerializableSchemaTree(list: List<SchemaTree.SchemaNodeData>) : Serializable {
  inner class SerializableSchemaNode(
    val tpeString: String,
    private val chFrom: Int,
    private val chTo: Int,
    val columnName: String = "",
    val primType: String = ""
  ) : Serializable {
    fun getChildren(): Collection<SerializableSchemaNode> =
      if (chFrom >= chTo) emptyList() else nodes.slice(IntRange(chFrom, chTo - 1))

    fun hasChildren(): Boolean = chFrom < chTo
  }

  private val nodes = list.map {
    SerializableSchemaNode(
      it.tpe?.toString() ?: "?",
      it.chFrom,
      it.chTo,
      it.tpe?.name ?: "",
      if (it.tpe?.isPrimitive == true) it.tpe.asPrimitiveType().primitiveTypeName.toString() else ""
    )
  }

  fun getRoot(): SerializableSchemaNode = if (nodes.isNotEmpty()) nodes[0] else SerializableSchemaNode("", 0, 0)
}

