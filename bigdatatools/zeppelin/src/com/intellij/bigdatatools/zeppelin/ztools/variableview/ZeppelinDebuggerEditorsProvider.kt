package com.intellij.bigdatatools.zeppelin.ztools.variableview

import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.xdebugger.XNamedTreeNode
import com.intellij.xdebugger.evaluation.InlineDebuggerHelper
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider

class ZeppelinDebuggerEditorsProvider : XDebuggerEditorsProvider() {
  override fun getFileType(): FileType = ZeppelinFileType

  override fun getInlineDebuggerHelper(): InlineDebuggerHelper = ZeppelinInlineDebuggerHelper.instance

  private class ZeppelinInlineDebuggerHelper : InlineDebuggerHelper() {
    override fun shouldEvaluateChildrenByDefault(node: XNamedTreeNode): Boolean = false

    companion object {
      val instance = ZeppelinInlineDebuggerHelper()
    }
  }
}