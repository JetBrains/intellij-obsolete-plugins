package com.intellij.bigdatatools.plugin.spark.services.node

import com.intellij.bigdatatools.plugin.spark.services.SparkJobServiceViewContributor
import com.intellij.execution.services.ServiceEventListener
import com.intellij.execution.services.ServiceViewDescriptor
import com.intellij.execution.services.ServiceViewProvidingContributor
import com.intellij.execution.services.SimpleServiceViewDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.util.Enumeration
import javax.swing.Icon
import javax.swing.tree.TreeNode

abstract class BdtBaseNode(val parent: BdtBaseNode?, val project: Project) : Comparable<BdtBaseNode>, TreeNode,
                                                                             Disposable,
                                                                             ServiceViewProvidingContributor<BdtBaseNode, BdtBaseNode> {
  abstract val label: String
  abstract val icon: Icon?

  protected var cachedChildren: List<BdtDriverNode>? = null

  override fun getServiceDescriptor(project: Project, service: BdtBaseNode) = service.getNodeViewDescriptor(project)
  override fun getViewDescriptor(project: Project): ServiceViewDescriptor = SimpleServiceViewDescriptor(label, icon)

  override fun compareTo(other: BdtBaseNode): Int = label.compareTo(other.label)
  abstract fun getNodeViewDescriptor(project: Project): ServiceViewDescriptor
  override fun getChildAt(childIndex: Int): BdtBaseNode? = cachedChildren?.get(childIndex)
  override fun getChildCount() = cachedChildren?.size ?: -1
  override fun getParent(): TreeNode? = parent
  override fun getIndex(node: TreeNode) = cachedChildren?.indexOf(node) ?: -1
  override fun getAllowsChildren(): Boolean = true
  override fun isLeaf(): Boolean = false
  override fun children(): Enumeration<out TreeNode>? = cachedChildren?.toEnumeration()

  abstract fun getChildren(): List<BdtDriverNode>

  fun refresh() {
    clearCache()
    SparkJobServiceViewContributor.Utils.sendEvent(ServiceEventListener.EventType.SERVICE_STRUCTURE_CHANGED,
                                                   this, SparkJobServiceViewContributor::class.java)
  }


  fun refreshChildren() {
    clearCache()
    SparkJobServiceViewContributor.Utils.sendEvent(ServiceEventListener.EventType.SERVICE_CHILDREN_CHANGED,
                                                   this, SparkJobServiceViewContributor::class.java)
  }

  fun getCachedOrLoadServices(project: Project) = cachedChildren?.takeIf { it.isNotEmpty() } ?: getServices(project)

  override fun getServices(project: Project): List<BdtDriverNode> {
    cachedChildren?.let {
      return it
    }

    val children = getChildren()
    cachedChildren = children
    return children.toMutableList()
  }

  override fun asService(): BdtBaseNode = this

  fun clearCache() {
    cachedChildren?.forEach {
      it.clearCache()
      Disposer.dispose(it)
    }
    cachedChildren = null
  }

  companion object {
    private fun List<BdtBaseNode>.toEnumeration(): Enumeration<out TreeNode> = object : Enumeration<TreeNode> {
      var iterator = this@toEnumeration.iterator()
      override fun hasMoreElements() = iterator.hasNext()
      override fun nextElement(): TreeNode = iterator.next()
    }
  }
}

