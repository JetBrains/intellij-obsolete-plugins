package com.intellij.bigdatatools.zeppelin.ztools.variableview

import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XValueChildrenList

class ZeppelinFrameAccessor(private val realRoot: ZeppelinDebugNode) {
  private var root: XCompositeNode? = null

  fun setCurrentRootNode(node: XCompositeNode) {
    root = node
  }

  fun loadFrame(): XValueChildrenList {
    val result = XValueChildrenList()
    realRoot.children?.forEach {
      result.add(it)
    }

    return result
  }
}