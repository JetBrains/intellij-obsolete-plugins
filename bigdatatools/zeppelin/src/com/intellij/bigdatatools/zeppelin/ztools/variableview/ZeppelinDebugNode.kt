package com.intellij.bigdatatools.zeppelin.ztools.variableview

import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.bigdatatools.zeppelin.ztools.dataframe.ZtoolsDataFrameUtils
import com.intellij.bigdatatools.zeppelin.ztools.dataframe.isDataFrame
import com.intellij.bigdatatools.zeppelin.ztools.dataframe.isInterpreterOrRoot
import com.intellij.bigdatatools.zeppelin.ztools.dataframe.isStructField
import com.intellij.bigdatatools.zeppelin.ztools.dataframe.isStructType
import com.intellij.bigdatatools.zeppelin.ztools.variableview.VariableView.Companion.ERROR_ROOT_NAME
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.util.NlsSafe
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace
import com.jetbrains.bigdatatools.common.BigdatatoolsCoreIcons
import com.jetbrains.bigdatatools.common.rfs.icons.RfsIcons
import javax.swing.Icon

class ZeppelinDebugNode(private val parent: ZeppelinDebugNode?,
                        name: String,
                        private var refName: String?,
                        var type: String? = null,
                        var value: String? = null,
                        var isDict: Boolean = false) : XNamedValue(name), Comparable<ZeppelinDebugNode> {
  var children: ArrayList<ZeppelinDebugNode>? = null
  var lazy = false
  var isResNode = false
  var length = -1


  val fullName: String = let {
    val prefix = parent?.let { "${it.fullName}." } ?: ""
    prefix + name
  }

  val fullRef: String = let {
    val parentRef = parent?.fullRef ?: ""
    parentRef + (refName ?: "")
  }


  fun clear() {
    type = null
    refName = null
    children = null
    lazy = false
    isResNode = false
    length = -1
    isDict = false
    value = null
  }


  override fun compareTo(other: ZeppelinDebugNode): Int {
    val isResNoteCompare = isResNode.compareTo(other.isResNode)
    return if (isResNoteCompare != 0)
      isResNoteCompare
    else name.compareTo(other.name)
  }

  fun addChild(child: ZeppelinDebugNode) {
    if (children == null) {
      children = ArrayList()
    }

    children!!.add(child)
  }

  override fun canNavigateToSource(): Boolean = false

  override fun computeChildren(node: XCompositeNode) =
    if (children != null) {
      val list = XValueChildrenList()
      children!!.forEach { list.add(it) }
      node.addChildren(list, true)
    }
    else {
      node.addChildren(XValueChildrenList.EMPTY, true)
    }

  override fun computePresentation(node: XValueNode, place: XValuePlace) {
    val textValue = computeTextValue()

    if (textValue.length > XValueNode.MAX_VALUE_LENGTH)
      node.setFullValueEvaluator(XZeppelinFullValueEvaluator(textValue))
    node.setPresentation(getValueIcon(), XZeppelinValuePresentation(textValue, computePresentableType()), children != null)
  }

  private fun computePresentableType(): String? = when {
    isStructField -> null
    isStructType -> "Columns: $length"
    length > -1 && type != null -> "$type: $length"
    length > -1 && type == null -> "<undefined>: $length"
    else -> type
  }

  @NlsSafe
  fun computeTextValue(): String = when {
    isDataFrame -> {
      val columns = ZtoolsDataFrameUtils.getSchemaInfo(this).schema.columns
      if (columns.isNotEmpty())
        "schema = " + columns.joinToString { it.toString() }
      else
        "<empty schema>"
    }
    isStructField -> {
      val struct = ZtoolsDataFrameUtils.parseStructField(this)
      if (struct != null)
        "${struct.name}: ${struct.tpe.presentableName} (${if (struct.nullable) "nullable" else "non-nullable"})"
      else
        "null"
    }
    isStructType -> ""
    isInterpreterOrRoot && name.endsWith("pyspark") -> ZepMessagesBundle.message("ztools.variable.root.pyspark")
    isInterpreterOrRoot && name.endsWith("spark") -> ZepMessagesBundle.message("ztools.variable.root.spark")
    isInterpreterOrRoot && name.endsWith("sql") -> ZepMessagesBundle.message("ztools.variable.root.sql")
    isInterpreterOrRoot && name == ERROR_ROOT_NAME -> ZepMessagesBundle.message("ztools.variable.root.error")
    length != -1 -> calculateValueToCollections()
    lazy -> "lazy"
    value == null && type == "Unit" -> ""
    value == null && children == null -> "null"
    value == null -> ""
    else -> value ?: "null"
  }

  private fun calculateValueToCollections(): String {
    val stringBuilder = StringBuilder()
    val maxLen = XValueNode.MAX_VALUE_LENGTH - 5
    val childrenNodes = children ?: ArrayList()
    for (i in childrenNodes.indices) {
      val child = childrenNodes[i]
      if (isDict)
        stringBuilder.append(child.name + ": " + child.computeTextValue())
      else
        stringBuilder.append(child.computeTextValue())
      if (i < childrenNodes.size - 1) {
        stringBuilder.append(", ")
      }
      if (stringBuilder.length > XValueNode.MAX_VALUE_LENGTH)
        break
    }
    val string = if (stringBuilder.length > XValueNode.MAX_VALUE_LENGTH - 2) {
      stringBuilder.toString().take(maxLen) + "..."
    }
    else
      stringBuilder.toString()

    return if (isDict)
      "{$string}"
    else
      "[$string]"
  }


  private fun getValueIcon(): Icon = when {
    isInterpreterOrRoot && name.endsWith("pyspark") -> Language.findLanguageByID("Python")?.associatedFileType?.icon
                                                       ?: AllIcons.Debugger.Value
    isInterpreterOrRoot && name.endsWith("spark") -> Language.findLanguageByID("Scala")?.associatedFileType?.icon ?: AllIcons.Debugger.Value
    isInterpreterOrRoot && name.endsWith("sql") -> Language.findLanguageByID("GenericSQL")?.associatedFileType?.icon
                                                   ?: AllIcons.Debugger.Value
    isInterpreterOrRoot && name == ERROR_ROOT_NAME -> AllIcons.General.Error
    isResNode -> AllIcons.Debugger.AddToWatch
    children == null -> {
      AllIcons.Debugger.Db_primitive
    }
    type != null && ("list" == type || "tuple" == type) -> {
      AllIcons.Debugger.Db_array
    }
    type == "Table" -> RfsIcons.META_TABLE_ICON
    type == "Database" -> BigdatatoolsCoreIcons.Nodes.Databases
    else -> {
      AllIcons.Debugger.Value
    }
  }
}