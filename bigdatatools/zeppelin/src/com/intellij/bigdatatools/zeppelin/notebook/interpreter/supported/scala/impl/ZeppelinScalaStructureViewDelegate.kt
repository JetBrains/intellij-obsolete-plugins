@file:Suppress("UnusedImport")

package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.scala.impl

import com.intellij.bigdatatools.zeppelin.structureview.ZeppelinStructureViewUtil
import com.intellij.pom.Navigatable
import org.jetbrains.plugins.scala.structureView.element.Element

class ZeppelinScalaStructureViewDelegate(private val delegateElement: Element) : Element by delegateElement, Navigatable {
  override fun navigate(requestFocus: Boolean) {
    ZeppelinStructureViewUtil.navigateImpl(delegateElement.element(), requestFocus)
  }

  override fun canNavigate(): Boolean = (delegateElement as? Navigatable)?.canNavigate() ?: false

  override fun canNavigateToSource(): Boolean = (delegateElement as? Navigatable)?.canNavigateToSource() ?: false
  //
  //override fun getChildren(): Array<TreeElement> {
  //  return delegateElement.getChildren().filter { it is Element }.map {
  //    ZeppelinScalaStructureViewDelegate(it as Element)
  //  }.toArray(emptyArray<TreeElement>())
  //}
}