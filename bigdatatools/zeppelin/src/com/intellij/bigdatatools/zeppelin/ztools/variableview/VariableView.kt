package com.intellij.bigdatatools.zeppelin.ztools.variableview

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.statistics.StateViewerUsageCollector
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsNoteController
import com.intellij.bigdatatools.zeppelin.ztools.dataframe.isStructType
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import com.intellij.xdebugger.impl.frame.XStandaloneVariablesView
import com.intellij.xdebugger.impl.ui.tree.nodes.XDebuggerTreeNode
import com.jetbrains.bigdatatools.common.ui.BdtJsonInfoDialog
import com.jetbrains.bigdatatools.common.ui.getCenterComponent
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants
import javax.swing.border.CompoundBorder
import kotlin.collections.set

class VariableView(val project: Project, val file: NotebookVirtualFile) : SimpleToolWindowPanel(false), Disposable {
  val root = ZeppelinDebugNode(null, "root", null)

  private val unresolvedRefNodes = mutableListOf<RefNode>()
  private val resolvedNodes = mutableMapOf<String, ZeppelinDebugNode>()

  private val stackFrame: ZeppelinStackFrame =
    ZeppelinStackFrame(project, ZeppelinFrameAccessor(root), ZeppelinStackFrameInfo("", ""), null)

  private val lineCache = ConcurrentHashMap<Int, String>()

  private var variablesView = object : XStandaloneVariablesView(project, ZeppelinDebuggerEditorsProvider(), stackFrame) {
    fun update() = buildTreeAndRestoreState(stackFrame)
  }

  private val updateInfoLabel = JLabel("", SwingConstants.RIGHT).also {
    val margin = JBUI.Borders.emptyRight(15)
    it.border = CompoundBorder(border, margin)
  }

  init {
    Disposer.register(this, variablesView)

    name = file.presentableName
    variablesView.tree.emptyText.apply {
      clear()
      appendText(ZepMessagesBundle.message("ztool.variable.view.empty.text"), SimpleTextAttributes.GRAYED_ATTRIBUTES)
      appendSecondaryText(ZepMessagesBundle.message("ztool.variable.view.refresh"), SimpleTextAttributes.LINK_ATTRIBUTES) {
        refreshVariables()
      }
    }

    //Expand 1st level of nodes.
    variablesView.tree.expandNodesOnLoad { node ->
      (node as? XDebuggerTreeNode)?.path?.pathCount == 2
    }

    // For user interaction sending small statistics message.
    variablesView.tree.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        val treePath = variablesView.tree.getPathForLocation(e.x, e.y) ?: return
        StateViewerUsageCollector.interactionEvent.log(project, StateViewerUsageCollector.getInterpreter(treePath))
      }
    })

    // variablesView.panel.border = BorderFactory.createEmptyBorder()
    (variablesView.panel.getCenterComponent() as? JScrollPane)?.let {
      it.border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
    }

    val panel = JPanel(BorderLayout()).apply {
      add(variablesView.panel, BorderLayout.CENTER)
      add(updateInfoLabel, BorderLayout.SOUTH)
    }

    panel.border = IdeBorderFactory.createBorder(SideBorder.LEFT)
    add(panel, BorderLayout.CENTER)

    val actionToolbar = createToolbar().apply { targetComponent = this@VariableView }
    toolbar = actionToolbar.component
    refreshInfo()
  }

  override fun dispose() = Unit

  fun isUpToDate(): Boolean = true

  fun isEmpty(interpreterCode: String): Boolean {
    val interpreterNode: ZeppelinDebugNode = root.children?.find { it.name == interpreterCode } ?: return true
    return interpreterNode.children.isNullOrEmpty()
  }

  fun setVariables(variables: Map<String, Any>, interpreterCode: String, isAppend: Boolean) {
    lineCache.clear()
    val interpreterNode: ZeppelinDebugNode = root.children?.find { it.name == interpreterCode } ?: ZeppelinDebugNode(root, interpreterCode,
                                                                                                                     null, null,
                                                                                                                     null).also {
      root.addChild(it)
    }

    if (!isAppend) {
      interpreterNode.children = null
    }

    unresolvedRefNodes.clear()
    resolvedNodes.clear()

    processObject(interpreterNode, variables)
    processRefNodes()
    processResNodes(interpreterNode)

    invokeAndWaitIfNeeded {
      variablesView.update()
    }
  }

  fun clearErrors(interpreterCode: String) {
    lineCache.clear()
    val errorRootNode: ZeppelinDebugNode = root.children?.find { it.name == ERROR_ROOT_NAME } ?: return
    val sourceNode = errorRootNode.children?.firstOrNull { it.name == interpreterCode } ?: return
    errorRootNode.children?.remove(sourceNode)
    if (errorRootNode.children?.isEmpty() == true) {
      root.children?.remove(errorRootNode)
    }

    invokeAndWaitIfNeeded {
      variablesView.update()
    }
  }

  fun addErrors(interpreterCode: String, errors: List<String>) {
    lineCache.clear()
    val interpreterNode: ZeppelinDebugNode = root.children?.find { it.name == ERROR_ROOT_NAME }
                                             ?: ZeppelinDebugNode(root, ERROR_ROOT_NAME, null, null, null).also {
                                               root.addChild(it)
                                             }
    interpreterNode.children = null
    parseNode(parent = interpreterNode, name = interpreterCode, refName = "",
              data = mapOf(VALUE to errors, LENGTH to errors.size.toDouble()))

    invokeAndWaitIfNeeded {
      variablesView.update()
    }
  }

  fun refreshInfo() {
    val source = file.getUserData(ZtoolsNoteController.ZTOOLS_LAST_UPDATE_SOURCE)
    val time = file.getUserData(ZtoolsNoteController.ZTOOLS_LAST_UPDATE_DATE) ?: let {
      updateInfoLabel.text = ZepMessagesBundle.message("ztools.variable.view.update.empty")
      return
    }

    val textCommon = ZepMessagesBundle.message("ztools.variable.view.update.text", time.toString())

    updateInfoLabel.text = "$textCommon " + when {
      source == null -> ZepMessagesBundle.message("ztools.variable.view.update.source.button")
      source.title?.isBlank() == false -> source.title!!
      else -> "${ZepMessagesBundle.message("paragraph.name")} ${source.indexInNote}"
    } + "."
  }

  fun cacheLineInfo(lineNumber: Int, info: String) {
    lineCache[lineNumber] = info
  }

  fun getCachedInfo(lineNumber: Int): String? = lineCache[lineNumber]

  @Suppress("UNCHECKED_CAST")
  private fun parseNode(parent: ZeppelinDebugNode?,
                        name: String,
                        refName: String,
                        data: Any?) {
    val realName = if (parent?.isStructType != true)
      name
    else {
      val map = data as? Map<String, Any>
      val value = map?.get(VALUE) as? Map<String, Any>
      value?.get("name") as? String ?: name
    }

    val existNode = parent?.children?.firstOrNull { realName == it.name }
    val node = existNode?.also { it.clear() } ?: parent?.let {
      val node = ZeppelinDebugNode(parent, realName, refName, null, null)
      parent.addChild(node)
      node
    } ?: root

    when {
      data.isPrimitive -> node.value = toViewString(data)
      data is Map<*, *> -> {
        val refLink = data[REF] as? String
        if (refLink != null && parent != null) {
          unresolvedRefNodes.add(RefNode(node, refLink))
          return
        }

        (data[TYPE] as? String)?.let {
          node.type = it
        }
        ((data[LENGTH] as? Double)?.toInt())?.let {
          node.length = it
        }
        node.lazy = data[LAZY] as? Boolean ?: false

        val keys = data[KEY] as? List<Any>
        val value = data[VALUE]
        when {
          value.isPrimitive -> processPrimitive(node, value)
          value is List<*> && keys is List<*> -> processMap(node, keys, value)
          value is List<*> -> processList(node, value)
          value is Map<*, *> -> processObject(node, value as Map<String, Any?>)
        }
      }
    }

    resolvedNodes[node.fullRef] = node
  }

  private fun createToolbar(): ActionToolbar {
    val refreshAction = DumbAwareAction.create(ZepMessagesBundle.message("ztools.note.action.refresh.title"), AllIcons.Actions.Refresh) {
      refreshVariables()
    }

    val openSettings = DumbAwareAction.create(ZepMessagesBundle.message("ztools.note.action.settings.title"), AllIcons.General.Settings) {
      val controller = ZtoolsNoteController.getFor(file) ?: error("StateViewer Note Controller is not found")
      controller.showSettings()

      StateViewerUsageCollector.settingsEvent.log()
    }

    val showResult = object : DumbAwareAction(ZepMessagesBundle.message("ztools.profiling.last.result"), null,
                                              AllIcons.FileTypes.Json) {

      override fun actionPerformed(e: AnActionEvent) {
        val debugInfo = ZtoolsNoteController.getFor(file)?.debugInfo ?: return
        BdtJsonInfoDialog(project, ZepMessagesBundle.message("ztools.profiling.last.result"), debugInfo).show()
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = ZtoolsNoteController.getFor(file)?.ztoolsConf?.profiling == true
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    return ToolbarUtils.createActionToolbar("BDTZeppelinVariableView", DefaultActionGroup(refreshAction, showResult, openSettings), true)
  }

  private fun refreshVariables() {
    val controller = ZtoolsNoteController.getFor(file) ?: error("StateViewer Note Controller is not found")
    controller.refresh()

    StateViewerUsageCollector.updateEvent.log(project)
  }

  private fun processObject(parent: ZeppelinDebugNode, data: Map<String, Any?>) =
    data.entries.forEach { entry ->
      val name = entry.key
      parseNode(parent = parent, name = name, refName = ".$name", data = entry.value)
    }

  private fun processList(root: ZeppelinDebugNode, list: List<*>) {
    if (root.type == null) {
      root.type = "Unrecognized List"
    }

    list.withIndex().forEach {
      parseNode(root, it.index.toString(), "[${it.index}]", it.value)
    }
    if (root.length != list.size) {
      val nextName = ZepMessagesBundle.message("ztools.variable.view.next.nodes.not.loaded.title")
      parseNode(root, nextName, "[$nextName]",
                ZepMessagesBundle.message("ztools.variable.view.next.nodes.not.loaded.value", root.length - list.size))
    }
  }

  private fun processMap(root: ZeppelinDebugNode, keys: List<*>, values: List<*>) {
    if (keys.size != values.size) {
      logger.warn("Keys and values differs in size in ${root.fullName}")
      return
    }

    root.isDict = true
    if (root.type == null) {
      root.type = "Unrecognized Map"
    }

    keys.zip(values).withIndex().forEach {
      parseNode(root, toViewString(it.value.first) ?: "", ".value[${it.index}]", it.value.second)
    }

    if (root.length != keys.size) {
      val nextName = ZepMessagesBundle.message("ztools.variable.view.next.nodes.not.loaded.title")
      parseNode(root, nextName, "[$nextName]",
                ZepMessagesBundle.message("ztools.variable.view.next.nodes.not.loaded.value", root.length - keys.size))
    }
  }

  private fun processPrimitive(node: ZeppelinDebugNode, value: Any?) {
    when {
      root.type != null -> Unit
      value is Boolean -> root.type = "Boolean"
      else -> root.type = "Unrecognized Primitive"
    }

    node.value = toViewString(value)
  }

  private fun processRefNodes() {
    unresolvedRefNodes.forEach { refNode ->
      val refName = "${root.fullRef}.${refNode.ref}"

      val sourceNode = resolvedNodes[refName] ?: let {
        logger.warn("Cannot resolve reference '${refNode.ref}' for ${refNode.node.fullName}")
        return@forEach
      }
      val node = refNode.node
      node.isResNode = sourceNode.isResNode
      node.lazy = sourceNode.lazy
      node.length = sourceNode.length
      node.value = sourceNode.value
      node.children = sourceNode.children
      node.type = sourceNode.type
    }
    unresolvedRefNodes.clear()
    resolvedNodes.clear()
  }

  private fun processResNodes(root: ZeppelinDebugNode) {
    if (root.children == null) return

    // getting all "res" nodes
    val regex = Regex("res[0-9]+")
    val resNodes = root.children?.filter { it.name.matches(regex) } ?: emptyList()
    if (resNodes.isEmpty())
      return
    // removing res nodes from parent
    resNodes.forEach { root.children!!.remove(it) }

    // creating fake res node and adding reverse sorted res nodes
    val resRoot = ZeppelinDebugNode(root, "res", "", "Grouped and sorted 'res' nodes")
    resRoot.children = ArrayList(resNodes.sortedWith(compareByDescending { it.name.substring(3).toInt() }))
    resRoot.isResNode = true

    root.children?.add(0, resRoot)
  }

  private fun toViewString(x: Any?): String? = when {
    x == null -> null
    x is String -> "\"$x\""
    x is Double && x.compareTo(x.toInt()) == 0 -> x.toInt().toString()
    else -> x.toString()
  }

  companion object {
    private val logger = Logger.getInstance(this::class.java)

    const val ERROR_ROOT_NAME = "StateViewer.errors"

    const val KEY = "key"
    const val VALUE = "value"
    const val TYPE = "type"
    const val REF = "ref"
    const val LENGTH = "length"
    const val LAZY = "lazy"
  }

  private val Any?.isPrimitive: Boolean
    get() = this is String || this == null || this is Double || this is Boolean

  private data class RefNode(val node: ZeppelinDebugNode, val ref: String)
}