package com.jetbrains.hadoop.monitoring.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.concurrency.Invoker
import com.intellij.util.concurrency.InvokerSupplier
import com.jetbrains.hadoop.monitoring.data.models.DataModelFs
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.LogFileInfo
import java.awt.Color
import java.awt.Component
import java.util.Enumeration
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

fun String.notEndsWith(suffix: String, ignoreCase: Boolean = false): Boolean {
  if (!ignoreCase)
    return !this.endsWith(suffix)
  else
    return !regionMatches(length - suffix.length, suffix, 0, suffix.length, ignoreCase = true)
}

class LogTreeCellRenderer : DefaultTreeCellRenderer() {
  override fun getTreeCellRendererComponent(tree: JTree?,
                                            value: Any?,
                                            sel: Boolean,
                                            expanded: Boolean,
                                            leaf: Boolean,
                                            row: Int,
                                            hasFocus: Boolean): Component {

    super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)

    val logNode = (value as? LogTreeNode)
    if (logNode?.error != null) {
      foreground = Color.red
      text = logNode.error
    }

    return this
  }
}

class ListEnumeration<T : Any>(private val list: List<T>) : Enumeration<T> {
  private var index = 0
  override fun hasMoreElements() = index < list.size
  override fun nextElement() = list[index++]
}

class LogTreeNode(private val fs: DataModelFs, val file: LogFileInfo?, private val parent: LogTreeNode?) : TreeNode {

  var error: String? = null
    private set

  private var childrenNodes: List<LogTreeNode>? = null

  private fun getChildrenNodes() =
    if (childrenNodes != null) {
      childrenNodes!!
    }
    else {
      childrenNodes = getChildren()
      childrenNodes!!
    }

  private val path
    get() = file?.path ?: "/logs"

  private fun getChildren() = try {
    error = null

    val promise = fs.getListFileInfo(path)
    promise.onError { }
    if (!ApplicationManager.getApplication().isWriteIntentLockAcquired)
      error("Block write thread")
    if (!ApplicationManager.getApplication().isReadAccessAllowed)
      error("Block read thread")

    promise.blockingGet(10000)!!.map { LogTreeNode(fs, it, this) }
  }
  catch (e: Throwable) {
    error = e.message ?: "Error getting $path"
    emptyList()
  }

  fun reset() {
    fs.resetPath(path)
    if (childrenNodes != null) {
      childrenNodes!!.forEach { it.reset() }
      childrenNodes = null
    }
  }

  fun resetContent() {
    fs.resetContent(path)
  }

  override fun children(): Enumeration<out TreeNode>? {
    return ListEnumeration(getChildrenNodes())
  }

  override fun isLeaf(): Boolean {
    return file?.path?.notEndsWith("/") ?: false
  }

  override fun getChildCount() = getChildrenNodes().size

  override fun getParent(): TreeNode? = parent
  override fun getChildAt(childIndex: Int): TreeNode = getChildrenNodes()[childIndex]
  override fun getIndex(node: TreeNode?): Int = getChildrenNodes().indexOf(node)
  override fun getAllowsChildren() = true

  override fun toString(): String {
    return file?.name ?: "logs"
  }
}

class LogsTreeModel(fs: DataModelFs) : DefaultTreeModel(LogTreeNode(fs, null, null)), Disposable, InvokerSupplier {

  private val invoker = Invoker.forBackgroundThreadWithReadAction(this)

  fun refresh() {
    (root as LogTreeNode).reset()
    nodeStructureChanged(root)
  }

  fun resetContentCache(node: LogTreeNode) {
    node.resetContent()
  }

  override fun getInvoker() = invoker

  override fun dispose() {}
}