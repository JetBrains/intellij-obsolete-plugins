package com.intellij.bigdatatools.zeppelin.ztools.variableview

import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValue
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.frame.XValueGroup
import javax.swing.Icon

class ZeppelinStackFrame(private val myProject: Project,
                         private val myDebugProcess: ZeppelinFrameAccessor,
                         private val myFrameInfo: ZeppelinStackFrameInfo,
                         private val position: XSourcePosition?) : XStackFrame() {
  override fun getEqualityObject(): Any = STACK_FRAME_EQUALITY_OBJECT

  override fun getSourcePosition(): XSourcePosition? = position

  override fun getEvaluator(): XDebuggerEvaluator? = null

  override fun customizePresentation(component: ColoredTextContainer) {
    component.setIcon(AllIcons.Debugger.Frame)

    if (position == null) {
      component.append(ZepMessagesBundle.message("ztools.frame.not.available"), SimpleTextAttributes.GRAY_ATTRIBUTES)
      return
    }

    val file = position.file
    val isExternal = ReadAction.compute<Boolean, RuntimeException> {

      val document = FileDocumentManager.getInstance().getDocument(file)
      if (document != null) {
        return@compute !ProjectRootManager.getInstance(myProject).fileIndex.isInContent(file)
      }
      else {
        return@compute true
      }
    }

    @Suppress("HardCodedStringLiteral") // Frame info is
    component.append(myFrameInfo.name, gray(isExternal))
    component.append(", ", gray(isExternal))
    component.append(position.file.name, gray(isExternal))
    component.append(":", gray(isExternal))
    component.append((position.line + 1).toString(), gray(isExternal))
  }

  override fun computeChildren(node: XCompositeNode) {
    if (node.isObsolete) return

    //node.
    myDebugProcess.setCurrentRootNode(node)
    executeOnPooledThread {
      try {
        val values = myDebugProcess.loadFrame()
        if (!node.isObsolete) {
          addChildren(node, values)
        }
      }
      catch (e: Exception) {
        if (!node.isObsolete) node.setErrorMessage("Unable to display frame variables")
        logger.warn(e)
      }
    }
  }

  private fun addChildren(node: XCompositeNode, children: XValueChildrenList?) {
    if (children == null) {
      node.addChildren(XValueChildrenList.EMPTY, true)
      return
    }
    val debuggerSettings = ZeppelinDebuggerSettings.instance
    val filteredChildren = XValueChildrenList()
    val returnedValues = HashMap<String, XValue>()

    val isSpecialEmpty = true

    for (i in 0 until children.size()) {
      val value = children.getValue(i)
      val name = children.getName(i)
      if (value is ZeppelinDebugNode) {
        if (!debuggerSettings.isSimplifiedView) {
          filteredChildren.add(name, value)
        }
        else {
          filteredChildren.add(name, value)
        }
      }
    }
    node.addChildren(filteredChildren, returnedValues.isEmpty() && isSpecialEmpty)
    if (returnedValues.isNotEmpty())
      addReturnedValuesGroup(node, returnedValues)
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)
    private val STACK_FRAME_EQUALITY_OBJECT = Any()
    const val RETURN_VALUES_GROUP_NAME = "Return Values"

    private fun gray(gray: Boolean): SimpleTextAttributes = if (!gray) {
      SimpleTextAttributes.REGULAR_ATTRIBUTES
    }
    else {
      if (SimpleTextAttributes.REGULAR_ATTRIBUTES.style and SimpleTextAttributes.STYLE_ITALIC != 0)
        SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES
      else
        SimpleTextAttributes.GRAYED_ATTRIBUTES
    }

    private fun addReturnedValuesGroup(node: XCompositeNode, returnedValues: Map<String, XValue>) {
      val group = ArrayList<XValueGroup>()
      group.add(object : XValueGroup(RETURN_VALUES_GROUP_NAME) {
        override fun computeChildren(node: XCompositeNode) {
          val list = XValueChildrenList()
          for ((key, value) in returnedValues) {
            list.add("$key()", value)
          }
          node.addChildren(list, true)
        }

        override fun getIcon(): Icon = AllIcons.Debugger.WatchLastReturnValue
      })
      node.addChildren(XValueChildrenList.topGroups(group), true)
    }
  }
}