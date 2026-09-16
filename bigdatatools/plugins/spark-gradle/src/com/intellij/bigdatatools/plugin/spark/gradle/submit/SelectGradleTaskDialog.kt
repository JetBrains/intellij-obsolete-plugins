package com.intellij.bigdatatools.plugin.spark.gradle.submit

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.view.ExternalProjectsStructure
import com.intellij.openapi.externalSystem.view.ExternalProjectsView
import com.intellij.openapi.externalSystem.view.ExternalProjectsViewAdapter
import com.intellij.openapi.externalSystem.view.ExternalProjectsViewImpl
import com.intellij.openapi.externalSystem.view.ExternalSystemNode
import com.intellij.openapi.externalSystem.view.ModuleNode
import com.intellij.openapi.externalSystem.view.ProjectNode
import com.intellij.openapi.externalSystem.view.TaskNode
import com.intellij.openapi.externalSystem.view.TasksNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.FilteringTreeModel
import com.intellij.ui.tree.TreeVisitor
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.Tree
import com.intellij.ui.treeStructure.filtered.FilteringTreeStructure
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleTaskData
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.function.Predicate
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

internal class SelectGradleTaskDialog(
  val project: Project,
  private val myTitle: @NlsContexts.DialogTitle String,
  private val artifacts: List<String>,
  private val initialSelected: String?,
  private val systemId: ProjectSystemId = GradleConstants.SYSTEM_ID,
  val nodeClasses: Array<Class<out ExternalSystemNode<*>>> = arrayOf(ProjectNode::class.java, ModuleNode::class.java, TasksNode::class.java,
                                                                     TaskNode::class.java, MyArtifactNode::class.java),
  private val mySelector: Predicate<in SimpleNode>? = null
) : DialogWrapper(project, false) {


  class MyTaskNode(externalProjectsView: ExternalProjectsView, val dataNode: DataNode<TaskData>) : TaskNode(externalProjectsView,
                                                                                                                  dataNode) {
    override fun isAlwaysLeaf(): Boolean {
      return false
    }
  }

  class MyArtifactNode(externalProjectsView: ExternalProjectsView,
                             dataNode: DataNode<ArtifactTaskData>) : ExternalSystemNode<ArtifactTaskData>(externalProjectsView, null,
                                                                                                          dataNode) {
    override fun getName(): String {
      return data?.artifact ?: ""
    }
  }

  class MyTasksNode(externalProjectsView: ExternalProjectsView, private val origin: DataNode<*>) : TasksNode(externalProjectsView,
                                                                                                             origin.children) {
    override fun doBuildChildren(): List<ExternalSystemNode<*>> {
      return super.doBuildChildren().map { result ->
        when (result) {
          is TaskNode -> MyTaskNode(externalProjectsView, origin.children.first { it.data === result.data } as DataNode<TaskData>)
          else -> result
        }
      }
    }
  }

  val myTree = Tree()
  private val comboboxModel = MutableCollectionComboBoxModel<String>()
  private val comboBox = ComboBox(comboboxModel)
  private lateinit var filteringTreeModel: FilteringTreeModel

  private fun createTree(): Tree {
    title = myTitle
    myTree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    object : DoubleClickListener() {
      override fun onDoubleClick(e: MouseEvent): Boolean {
        handleDoubleClickOrEnter(myTree.getClosestPathForLocation(e.x, e.y))
        return false
      }
    }.installOn(myTree)
    myTree.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ENTER && myTree.selectionPaths?.size == 1) {
          handleDoubleClickOrEnter()
        }
      }
    })
    val projectsView = ExternalProjectsManagerImpl.getInstance(project).getExternalProjectsView(systemId) ?: run {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(systemId.readableName)
      if (toolWindow is ToolWindowEx) {
        ExternalProjectsViewImpl(disposable, project, toolWindow, systemId)
      }
      else null
    }
    if (projectsView != null) {
      val treeStructure: ExternalProjectsStructure = object : ExternalProjectsStructure(project, myTree) {
        override fun getVisibleNodesClasses(): Array<Class<out ExternalSystemNode<*>>> {
          return nodeClasses
        }
      }
      Disposer.register(myDisposable, treeStructure)
      treeStructure.init(object : ExternalProjectsViewAdapter(projectsView) {
        override fun getStructure(): ExternalProjectsStructure {
          return treeStructure
        }

        override fun createNodes(externalProjectsView: ExternalProjectsView,
                                 parent: ExternalSystemNode<*>?,
                                 dataNode: DataNode<*>): List<ExternalSystemNode<*>> {
          if (parent is MyTaskNode) {
            val artifactTaskDataDataNode = ExternalSystemApiUtil.find(parent.dataNode, ArtifactTaskData.KEY) ?: return emptyList()
            if (artifactTaskDataDataNode.data.artifact == null) return emptyList()
            return listOf(MyArtifactNode(externalProjectsView, artifactTaskDataDataNode))
          }
          return super.createNodes(externalProjectsView, parent, dataNode).map {
            when (it) {
              is TasksNode -> MyTasksNode(externalProjectsView, dataNode)
              else -> it
            }
          }
        }

        override fun updateUpTo(node: ExternalSystemNode<*>?) {
          treeStructure.updateUpTo(node)
        }

        override fun getGroupTasks(): Boolean = false
        override fun useTasksNode(): Boolean = true
      })
      val projectsData = ProjectDataManager.getInstance().getExternalProjectsData(project, systemId)
      val dataNodes: List<DataNode<ProjectData>?> = projectsData.mapNotNull { it.externalProjectStructure }
      treeStructure.updateProjects(dataNodes)
      filteringTreeModel = FilteringTreeModel.createModel(treeStructure, {
        when (it) {
          is MyArtifactNode -> {
            comboboxModel.selected == null || it.data?.artifact == comboboxModel.selected
          }
          is MyTaskNode -> {
            val taskArtifact = ExternalSystemApiUtil.find(it.dataNode, ArtifactTaskData.KEY)
            comboboxModel.selected == null || taskArtifact == null || taskArtifact.data.artifact == comboboxModel.selected
          }
          else -> false
        }
      }, myDisposable)
      myTree.model = AsyncTreeModel(filteringTreeModel, myDisposable)
      filteringTreeModel.updateTree(myTree, true, null)
      TreeUtil.expandAll(myTree)
      if (mySelector != null) {
        TreeUtil.promiseSelect(myTree) { path: TreePath? ->
          val node = TreeUtil.getLastUserObject(
            SimpleNode::class.java, path)
          if (node != null && mySelector.test(node)) TreeVisitor.Action.INTERRUPT else TreeVisitor.Action.CONTINUE
        }
      }
    }
    return myTree
  }

  override fun getPreferredFocusedComponent(): JComponent = myTree

  fun handleDoubleClickOrEnter(treePath: TreePath? = myTree.selectionPath) {
    when (selectedNode(treePath ?: return)) {
      is MyTaskNode -> {
        myTree.selectionModel.selectionPath = treePath
        doOKAction()
      }
      is MyArtifactNode -> {
        myTree.selectionModel.selectionPath = treePath.parentPath
        doOKAction()
      }
    }
  }

  private fun selectedNode(treePath: TreePath): ExternalSystemNode<*>? {
    return ((treePath.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? FilteringTreeStructure.FilteringNode)?.delegate as? ExternalSystemNode<*>
  }

  fun showAndGetResult(): GradleSelectedArtifactInfo? {
    init()
    if (!showAndGet()) return null
    val artifact = comboboxModel.selected ?: return null
    return when (val taskNode = myTree.selectionPath?.let { selectedNode(it) }) {
      is MyTaskNode -> {
        val gradleTaskData = GradleTaskData(taskNode.dataNode, taskNode.moduleOwnerName)
        GradleSelectedArtifactInfo(artifact, gradleTaskData.name, gradleTaskData.data.linkedExternalProjectPath)
      }
      else -> GradleSelectedArtifactInfo(artifact, null, null)
    }
  }

  private fun createCombobox(): JComponent {
    comboboxModel.addAll(0, artifacts)
    comboboxModel.selectedItem = initialSelected
    comboBox.addItemListener {
      if (it.stateChange == ItemEvent.SELECTED || it.stateChange == ItemEvent.DESELECTED) {
        filteringTreeModel.updateTree(myTree, true, null)
      }
    }
    return comboBox
  }

  override fun createCenterPanel(): JComponent {
    val rootPanel = JPanel(BorderLayout())
    val artifactCombobox = createCombobox()
    rootPanel.add(artifactCombobox, BorderLayout.NORTH)
    val pane = ScrollPaneFactory.createScrollPane(createTree())
    pane.preferredSize = JBUI.size(320, 400)
    rootPanel.add(pane, BorderLayout.CENTER)
    return rootPanel
  }
}