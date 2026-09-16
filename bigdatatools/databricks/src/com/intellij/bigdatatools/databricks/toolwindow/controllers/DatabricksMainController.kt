package com.intellij.bigdatatools.databricks.toolwindow.controllers

import com.intellij.bigdatatools.databricks.actions.DatabricksAbstractRunAction
import com.intellij.bigdatatools.databricks.rfs.DatabricksConnectionData
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver.Companion.isConfiguration
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver.Companion.isServerRunList
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver.Companion.isWorkflowRunList
import com.intellij.bigdatatools.databricks.run.direct.DatabricksServerRunner
import com.intellij.bigdatatools.databricks.run.workflow.DatabricksWorkflowRunner
import com.intellij.bigdatatools.databricks.toolwindow.configuration.DbConfigurationController
import com.intellij.bigdatatools.databricks.toolwindow.controllers.server.ServerRunController
import com.intellij.bigdatatools.databricks.toolwindow.controllers.server.ServerRunDetailsController
import com.intellij.bigdatatools.databricks.toolwindow.controllers.workflow.WorkflowController
import com.intellij.bigdatatools.databricks.toolwindow.controllers.workflow.WorkflowDetailsController
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.GroupHeaderSeparator
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.MainTreeController
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.ui.getCenterComponent
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent

internal class DatabricksMainController(project: Project, connectionData: DatabricksConnectionData) : MainTreeController<DatabricksConnectionData, DatabricksDriver>(project, connectionData) {
  override val dataManager = driver.dataManager

  private val workflowController = WorkflowController().also { Disposer.register(this, it) }
  private val serverRunController = ServerRunController().also { Disposer.register(this, it) }
  private val configurationController = DbConfigurationController(project, driver.dataManager).also { Disposer.register(this, it) }
  private val serverRunDetailsController = ServerRunDetailsController(project, dataManager).also { Disposer.register(this, it) }
  private val workflowRunDetailsController = WorkflowDetailsController(project, dataManager).also { Disposer.register(this, it) }

  private val fileSelected = AtomicBooleanProperty(false)

  private val workflowButton = JButton(AllIcons.Debugger.ThreadRunning).apply {
    minimumSize = ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    addActionListener {
      val editor = FileEditorManager.getInstance(project).selectedEditor as? TextEditor ?: return@addActionListener
      val file = editor.file
      val dataManager = DatabricksAbstractRunAction.getDataManager(project) ?: return@addActionListener
      val runner = DatabricksWorkflowRunner(project, dataManager)
      DatabricksAbstractRunAction.run(project, editor, file, dataManager, runner)
    }
  }

  private val serverButton = JButton(AllIcons.Debugger.ThreadRunning).apply {
    minimumSize = ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    addActionListener {
      val editor = FileEditorManager.getInstance(project).selectedEditor as? TextEditor ?: return@addActionListener
      val file = editor.file
      val dataManager = DatabricksAbstractRunAction.getDataManager(project) ?: return@addActionListener
      val runner = DatabricksServerRunner(project, dataManager)
      DatabricksAbstractRunAction.run(project, editor, file, dataManager, runner)
    }
  }

  init {
    init()

    project.messageBus.connect(this).subscribe<FileEditorManagerListener>(
      FileEditorManagerListener.FILE_EDITOR_MANAGER,
      object : FileEditorManagerListener {
        override fun selectionChanged(event: FileEditorManagerEvent) {
          updateRunButtons()
        }
      }
    )
  }

  override fun dispose() = Unit

  private fun updateRunButtons() {
    val selectedFile = FileEditorManager.getInstance(project).selectedEditor?.file
    fileSelected.set(selectedFile != null)
    selectedFile ?: return

    workflowButton.text = DatabricksBundle.message("action.run.as.workflow.text", selectedFile.name)
    serverButton.text = DatabricksBundle.message("action.run.as.shell.text", selectedFile.name)

    val isScratchFile = ScratchUtil.isScratch(selectedFile)
    if (isScratchFile) {
      workflowButton.isEnabled = false
      serverButton.isEnabled = false

      workflowButton.toolTipText = DatabricksBundle.message("run.section.scratch.file")
      serverButton.toolTipText = DatabricksBundle.message("run.section.scratch.file")
    }
    else {
      workflowButton.isEnabled = selectedFile.extension in listOf("py", "ipynb")
      serverButton.isEnabled = selectedFile.extension in listOf("py")

      workflowButton.toolTipText = if (workflowButton.isEnabled) null else DatabricksBundle.message("run.section.unsupported.type.workflow")
      serverButton.toolTipText = if (serverButton.isEnabled) null else DatabricksBundle.message("run.section.unsupported.type.server")
    }

    workflowButton.repaint()
    serverButton.repaint()
  }

  override fun selectDefaultPath() {
    myTree.selectionPath = treeModel.getTreePath(DatabricksDriver.confPath)
  }

  override fun createToolbar() = null

  override fun setupDriverSpecificTreeInit() {
    details.add(configurationController.getComponent(), DatabricksGroupType.CONF.name)
    details.add(workflowController.getComponent(), DatabricksGroupType.WORKFLOW_LIST.name)
    details.add(serverRunController.getComponent(), DatabricksGroupType.SERVER_RUNS_LIST.name)
    details.add(workflowRunDetailsController.getComponent(), DatabricksGroupType.WORKFLOW_RUN_DETAILS.name)
    details.add(serverRunDetailsController.getComponent(), DatabricksGroupType.SERVER_RUN_DETAILS.name)
  }

  override fun createNormalPanel(): OnePixelSplitter {
    return super.createNormalPanel().apply {
      proportion = 0.3f
      setAndLoadSplitterProportionKey("databricks.toolwindow.splitter")
    }
  }

  override fun createTreePanel(): SimpleToolWindowPanel {
    val panel = super.createTreePanel()
    (panel.getCenterComponent() as? JComponent)?.border = BorderFactory.createEmptyBorder()

    val bottomPanel = panel {
      row {
        cell(GroupHeaderSeparator(JBUI.emptyInsets()).apply {
          caption = DatabricksBundle.message("run.buttons.section")
        }).align(AlignX.FILL)
      }
      row { cell(workflowButton).align(AlignX.FILL) }.visibleIf(fileSelected)
      row { cell(serverButton).align(AlignX.FILL) }.visibleIf(fileSelected)

      row { comment(DatabricksBundle.message("no.file.selected")).align(Align.CENTER).resizableColumn() }.resizableRow()
        .visibleIf(fileSelected.not())
    }
    panel.add(bottomPanel, BorderLayout.SOUTH)

    updateRunButtons()

    return panel
  }

  override fun showDetailsComponent(rfsPath: RfsPath?) {
    when {
      rfsPath == null || rfsPath.isRoot -> showDetailsComponent(DatabricksGroupType.CONF)
      rfsPath.isConfiguration -> showDetailsComponent(DatabricksGroupType.CONF)
      rfsPath.isWorkflowRunList -> showDetailsComponent(DatabricksGroupType.WORKFLOW_LIST)
      rfsPath.isServerRunList -> showDetailsComponent(DatabricksGroupType.SERVER_RUNS_LIST)
      rfsPath.parent?.isWorkflowRunList == true -> {
        showDetailsComponent(DatabricksGroupType.WORKFLOW_RUN_DETAILS)
        workflowRunDetailsController.setDetailsId(rfsPath.name.toLong())
      }
      rfsPath.parent?.isServerRunList == true -> {
        showDetailsComponent(DatabricksGroupType.SERVER_RUN_DETAILS)
        serverRunDetailsController.setDetailsId(rfsPath.name)
      }
    }
  }

  private fun showDetailsComponent(selectedValue: DatabricksGroupType?) = detailsLayout.show(details, selectedValue?.name)
}