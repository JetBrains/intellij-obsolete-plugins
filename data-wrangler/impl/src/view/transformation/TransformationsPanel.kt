package com.intellij.dataWrangler.impl.view.transformation

import com.intellij.dataWrangler.executor.DataWranglerContext
import com.intellij.dataWrangler.impl.DataWranglerBundle
import com.intellij.dataWrangler.impl.ui.DataWranglerUiSession
import com.intellij.dataWrangler.impl.view.transformation.TransformationsNode.AIActionNodeDescriptor
import com.intellij.dataWrangler.impl.view.transformation.TransformationsNode.CommandNodeDescriptor
import com.intellij.dataWrangler.impl.view.transformation.TransformationsNode.RootNodeDescriptor
import com.intellij.dataWrangler.impl.view.transformation.TransformationsTree.Companion.CLICK_TO_SWIPE_COUNT
import com.intellij.dataWrangler.llm.DWCommandAction
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.dataWrangler.operations.TransformationStep
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBCardLayout
import com.intellij.ui.JBCardLayout.SwipeDirection
import com.intellij.ui.PopupHandler
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.asSafely
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.launchOnShow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.awt.BorderLayout
import java.awt.event.InputEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
import javax.swing.SwingConstants
import javax.swing.SwingUtilities


internal class TransformationsPanel<C : DataWranglerContext>(private val session: DataWranglerUiSession<C>)
  : JBPanel<TransformationsPanel<C>>(BorderLayout()) {

  val project = session.tableViewer.getGrid().project

  /**
   * For switching back between a tree of commands choices and its parameters
   */
  private val cardLayout: JBCardLayout = JBCardLayout()
  private val cardPanel = JPanel(cardLayout)

  val treePanel: JPanel
  val treeSearchField: SearchTextField
  var parameterPanel: ParameterPanel<*, C>

  private val paramsScrollPane: JBScrollPane = JBScrollPane(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER)

  private val _treeEventFlow = MutableSharedFlow<TreeViewEvents<C>>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  val treeEventFlow: SharedFlow<TreeViewEvents<C>> = _treeEventFlow.asSharedFlow()

  private val _parameterEventFlow = MutableSharedFlow<ParameterViewEvents<C>>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  val parameterEventFlow: SharedFlow<ParameterViewEvents<C>> = _parameterEventFlow.asSharedFlow()

  init {
    val commands = session.backendSession.getEngine().commandsFactories().value
    val commandActions: List<DWCommandAction> = DWCommandAction.EP.extensionList.filter { it.isAvailable(project) }

    // tree panels
    treePanel = JPanel(BorderLayout())
    val treeView = createTreeView()

    val rootDesc = RootNodeDescriptor(commandActions, commands)
    val root = TransformationsNode(rootDesc)
    val filterTree = TransformationsFilterTree(project, treeView, root)
    PopupHandler.installPopupMenu(filterTree.tree, "DataWrangler.Operations.Popup", "DWOperationsPopup")
    treeSearchField = filterTree.installSearchField()
    val group = ActionManager.getInstance().getAction("DataWrangler.Operations.Toolbar") as ActionGroup
    val toolbar = ActionUtil.createToolbarComponent(treeView, "DWOperationsToolbar", group, true)

    val treeScrollPane = JBScrollPane(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER)
    treePanel.add(
      JBUI.Panels.simplePanel()
        .addToCenter(treeSearchField)
        .addToRight(toolbar),
      BorderLayout.NORTH)
    treeScrollPane.setViewportView(treeView)
    treePanel.add(treeScrollPane, BorderLayout.CENTER)

    // parameter panels
    parameterPanel = ParameterPanel(commands.first(), session)

    // combine both and have it in cardPanel
    cardPanel.add(treePanel, TREE_CARD)
    paramsScrollPane.setViewportView(createMainDialogPanel(parameterPanel))
    cardPanel.add(paramsScrollPane, PARAMS_CARD)
    add(cardPanel)
    treePanel.launchOnShow(javaClass.name) {
      session.backendSession.getEngine().commandsFactories().collectLatest {
        if (rootDesc.update(it)) {
          val state = TreeState.createOn(filterTree.tree, root)
          filterTree.searchModel.updateStructure()
          state.applyTo(filterTree.tree)
        }
      }
    }
  }

  private fun createMainDialogPanel(parameterPanel: ParameterPanel<*, C>, isReadOnly: Boolean = false): DialogPanel {
    val dialogPanel = parameterPanel.getPanel()
    return panel {
      row {
        bottomGap(BottomGap.NONE)
        topGap(TopGap.NONE)
        cell(createBackButton()).align(AlignX.LEFT)
      }
      row {
        topGap(TopGap.NONE)
        cell(dialogPanel).align(AlignX.FILL)
      }
      if (!isReadOnly) {
        row {
          cell(createApplyButton()).align(AlignX.LEFT)
        }
      }
    }.apply {
      border = JBUI.Borders.empty(UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP)
      registerIntegratedPanel(dialogPanel)
    }
  }

  private fun createApplyButton(): JButton = JButton(DataWranglerBundle.message("dw.button.apply")).apply {
    addActionListener { parameterApplyHandler() }
  }

  fun <P : Any> showParameterPreview(step: TransformationStep<P, C>?) {
    if (step == null) {
      swipeToTreeView()
      return
    }
    updateParametersPanel(step.factory, step.params)
    swipeToParameterPanel()
  }

  private fun <P : Any> updateParametersPanel(commandFactory: CommandFactory<P, C>, params: P? = null) {
    tryDisposeParametersPanel(parameterPanel)
    val readOnly: Boolean
    // Using readOnly here is temporary. We will implement edit step functionality
    if (params != null) { // create a read-only panel widget from params
      parameterPanel = ParameterPanel(commandFactory, params, session, readOnly = true)
      readOnly = true
    }
    else { // create a panel with new params of commandFactory
      parameterPanel = ParameterPanel(commandFactory, session, readOnly = false)
      readOnly = false
    }
    paramsScrollPane.setViewportView(createMainDialogPanel(parameterPanel, readOnly))
  }

  private fun tryDisposeParametersPanel(parameters: ParameterPanel<*, C>) {
    Disposer.dispose(parameters)
  }

  private fun parameterApplyHandler() {
    parameterPanel.getPanel().apply()
    val step = parameterPanel.createTransformationStep()

    val event = ParameterViewEvents.ParameterEditFinished(step)
    _parameterEventFlow.tryEmit(event)
    //session.backendSession.runTransformation(step)
    swipeToTreeView()
  }

  private fun swipeToTreeView() {
    cardLayout.swipe(cardPanel, TREE_CARD, SwipeDirection.BACKWARD)
    treeSearchField.text = ""

    val event = TreeViewEvents.CommandPickStarted<C>()
    _treeEventFlow.tryEmit(event)
  }

  private fun swipeToParameterPanel() {
    cardLayout.swipe(cardPanel, PARAMS_CARD, SwipeDirection.FORWARD)

    val event = ParameterViewEvents.ParameterEditStarted<C>()
    _parameterEventFlow.tryEmit(event)
  }

  private fun createTreeView() = TransformationsTree().apply {
    // add listeners
    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent?) {
        if (e != null && SwingUtilities.isLeftMouseButton(e) && e.clickCount == CLICK_TO_SWIPE_COUNT) {
          val myTree = this@apply
          val path = myTree.selectionPath ?: return
          perform(e, path.lastPathComponent)
        }
      }
    })

    addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent?) {
        if (e?.keyCode == KeyEvent.VK_ENTER) {
          val myTree = this@apply
          perform(e, myTree.lastSelectedPathComponent)
        }
      }
    })
  }

  private fun perform(e: InputEvent, pathComponent: Any?) {
    val node = pathComponent.asSafely<TransformationsNode>()
    node?.perform(e)
  }

  private fun TransformationsNode.perform(e: InputEvent) = when (val item = userObject) {
    is AIActionNodeDescriptor -> {
      item.commandAction.perform( createActionEvent(e))
      e.consume()
    }
    is CommandNodeDescriptor<*> -> item.asSafely<CommandNodeDescriptor<C>>()?.let { commandItem ->
      updateParametersPanel(commandItem.command)
      swipeToParameterPanel()
      e.consume()
    }
    else -> {}
  }

  private fun createActionEvent(inputEvent: InputEvent): AnActionEvent {
    val component = inputEvent.component
    val dataContext = DataManager.getInstance().getDataContext(component)
    return AnActionEvent.createEvent(
      dataContext,
      null,
      ActionPlaces.UNKNOWN,
      ActionUiKind.NONE,
      inputEvent,
    )
  }

  private fun createBackButton(): JButton = JButton(DataWranglerBundle.message("label.transformation.panel.back.to.transformations"), AllIcons.Actions.Back).apply {
    horizontalAlignment = SwingConstants.LEFT
    verticalAlignment = SwingConstants.TOP
    border = JBUI.Borders.empty()
    isBorderPainted = false
    addActionListener {
      swipeToTreeView()
    }
  }

  companion object {
    private const val TREE_CARD = "tree"
    private const val PARAMS_CARD = "params"
  }
}

interface TreeViewEvents<C : DataWranglerContext> {
  class CommandPickStarted<C : DataWranglerContext> : TreeViewEvents<C>
}

interface ParameterViewEvents<C : DataWranglerContext> {
  class ParameterEditStarted<C : DataWranglerContext> : ParameterViewEvents<C>
  class ParameterEditFinished<C : DataWranglerContext>(val step: TransformationStep<*, C>) : ParameterViewEvents<C>
}
