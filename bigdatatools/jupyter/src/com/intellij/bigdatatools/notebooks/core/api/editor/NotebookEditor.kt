package com.intellij.bigdatatools.notebooks.core.api.editor

import com.intellij.bigdatatools.notebooks.core.api.NotebookDataProvider
import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.impl.document.NoteDocumentChangeListener
import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.layout.migLayout.createLayoutConstraints
import com.intellij.ui.layout.migLayout.patched.MigLayout
import com.intellij.util.ui.JBUI
import com.jetbrains.bigdatatools.common.delegate.Delegate
import com.jetbrains.bigdatatools.common.util.ToolbarUtils
import net.miginfocom.layout.ConstraintParser
import javax.swing.JComponent
import javax.swing.JPanel

abstract class NotebookEditor(private val sourceEditor: TextEditor,
                              private val editorName: String) : TextEditor by sourceEditor {
  val project = editor.project ?: error("Project is not found")

  private var actionListeners = listOf<NoteEditorActionListener>()
  open val note: BasicNotebook = file.notebook

  val deselectNotifier = Delegate<Unit, Unit>()

  val toolbarActionGroup = DefaultActionGroup()
  val toolbar = ToolbarUtils.createActionToolbar("TextEditorWithPreviewLeft", toolbarActionGroup, true)

  private val component by lazy {

    val toolbarPanel = JPanel(MigLayout(createLayoutConstraints(0, 0).noVisualPadding().fill(),
                                        ConstraintParser.parseColumnConstraints("[grow][pref!]"))).apply {
      border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
      toolbar.targetComponent = this
      add(toolbar.component)
      val rightToolbar = ToolbarUtils.createActionToolbar(targetComponent = this, "TextEditorWithPreviewRight", getRightAlignedActions(),
                                                          horizontal = true).apply {
        layoutStrategy = ToolbarLayoutStrategy.NOWRAP_STRATEGY
      }
      add(rightToolbar.component)
    }

    val component = NotebookDataProvider.wrapComponent(
      sourceEditor.component,
      project, editor, note, cell = null, helpId = HELP_TOPIC_ID)
    JBUI.Panels.simplePanel(component).addToTop(toolbarPanel).apply {
      toolbar.targetComponent = this
    }
  }

  init {
    val notebookVirtualFile = sourceEditor.file as NotebookVirtualFile
    NoteDocumentChangeListener.registerNoteListener(notebookVirtualFile, project, notebookVirtualFile.notebook, editor.document)

    @Suppress("LeakingThis")
    Disposer.register(this, sourceEditor)
  }

  open fun getRightAlignedActions(): List<AnAction> = listOf(ActionManager.getInstance().getAction("NoteToggleLineWrap"),
                                                             ActionManager.getInstance().getAction("ToggleMinimap"))

  abstract fun getDefaultActions(): List<AnAction>

  fun setExecutorToolbarActions(actions: List<AnAction>) {
    toolbarActionGroup.removeAll()
    toolbarActionGroup.addAll(getDefaultActions())
    toolbarActionGroup.addAll(actions)
  }

  fun actionNotify(body: (NoteEditorActionListener) -> Unit) {
    if (!editor.document.isWritable) {
      invokeLater {
        Messages.showErrorDialog(NoteMessagesBundle.message("action.error.readonly.text", editorName),
                                 NoteMessagesBundle.message("action.error.title"))
      }
      return
    }
    actionListeners.forEach {
      try {
        body(it)
      }
      catch (t: Throwable) {
        logger.error(t)
      }
    }
  }

  fun addActionListener(actionListener: NoteEditorActionListener) {
    actionListeners = actionListeners + actionListener
  }

  fun removeActionListener(actionListener: NoteEditorActionListener) {
    actionListeners = actionListeners - actionListener
  }

  override fun getFile(): NotebookVirtualFile = sourceEditor.file as NotebookVirtualFile
  override fun deselectNotify() = deselectNotifier.notify(Unit)
  override fun getComponent(): JComponent = component

  @Suppress("HardCodedStringLiteral")
  override fun getName(): String = editorName
  override fun getStructureViewBuilder(): StructureViewBuilder? = sourceEditor.structureViewBuilder
  override fun getState(level: FileEditorStateLevel) = sourceEditor.getState(level)
  override fun setState(state: FileEditorState, exactState: Boolean) = sourceEditor.setState(state, exactState)

  fun getSourceEditor(): TextEditor = sourceEditor

  companion object {
    private const val HELP_TOPIC_ID = "big.data.tools.notebooks.running"

    private val logger = Logger.getInstance(this::class.java)
  }
}