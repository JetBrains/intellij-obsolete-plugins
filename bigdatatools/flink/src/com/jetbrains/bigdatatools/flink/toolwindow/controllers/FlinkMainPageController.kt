package com.jetbrains.bigdatatools.flink.toolwindow.controllers

import com.intellij.bigdatatools.coreUi.fields.CustomListCellRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBList
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.ComponentController
import com.jetbrains.bigdatatools.flink.data.FlinkDataManager
import com.jetbrains.bigdatatools.flink.rfs.FlinkConnectionData
import com.jetbrains.bigdatatools.flink.toolwindow.controllers.jars.FlinkJarsController
import com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobmanager.FlinkJobMangerController
import com.jetbrains.bigdatatools.flink.toolwindow.controllers.jobs.FlinkJobsController
import com.jetbrains.bigdatatools.flink.toolwindow.controllers.taskmanager.FlinkTaskManagersController
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.DefaultListModel
import javax.swing.DefaultListSelectionModel
import javax.swing.JPanel

class FlinkMainPageController(project: Project, private val connectionData: FlinkConnectionData) : ComponentController {
  private val dataManager = FlinkDataManager.getInstance(connectionData.innerId, project) ?: error("Data Manager is not initialized")

  private val jarsController = FlinkJarsController(project, dataManager)
  private val jobsController = FlinkJobsController(project, dataManager)
  private val taskManagersController = FlinkTaskManagersController(project, dataManager)
  private val jobManagerController = FlinkJobMangerController(project, dataManager)

  private val detailsLayout = CardLayout()
  private val details = JPanel(detailsLayout)
  private val panel = createPanel()

  init {
    Disposer.register(this, jarsController)
    Disposer.register(this, jobsController)
    Disposer.register(this, jobManagerController)
    Disposer.register(this, taskManagersController)
  }

  override fun dispose() {}

  override fun getComponent() = panel

  private fun showDetails(selectedValue: FlinkControllerType) {
    detailsLayout.show(details, selectedValue.name)
  }

  private fun createPanel(): JPanel {
    val model = DefaultListModel<FlinkControllerType>()
    if (connectionData.isFlinkHistory) {
      model.addElement(FlinkControllerType.JOBS)
    }
    else {
      FlinkControllerType.entries.forEach {
        model.addElement(it)
      }
    }

    val list = JBList(model).apply {
      cellRenderer = CustomListCellRenderer<FlinkControllerType> { it.value }

      selectionMode = DefaultListSelectionModel.SINGLE_SELECTION
      selectedIndex = 0

      addListSelectionListener { e ->
        if (e.valueIsAdjusting)
          return@addListSelectionListener
        showDetails(selectedValue)
      }
    }

    details.add(jarsController.getComponent(), FlinkControllerType.JARS.name)
    details.add(jobsController.getComponent(), FlinkControllerType.JOBS.name)
    details.add(jobManagerController.getComponent(), FlinkControllerType.JOB_MANAGER.name)
    details.add(taskManagersController.getComponent(), FlinkControllerType.TASK_MANAGER.name)
    showDetails(FlinkControllerType.JOBS)

    val leftPanel = JPanel(BorderLayout()).apply {
      add(ScrollPaneFactory.createScrollPane(list, true), BorderLayout.CENTER)
    }

    return OnePixelSplitter().apply {
      proportion = 0.1f
      dividerPositionStrategy = Splitter.DividerPositionStrategy.KEEP_FIRST_SIZE
      firstComponent = leftPanel
      secondComponent = details
    }
  }

  private enum class FlinkControllerType(val value: String) {
    JOBS(FlinkMessagesBundle.message("main.page.jobs")),
    TASK_MANAGER(FlinkMessagesBundle.message("main.page.taskManagers")),
    JOB_MANAGER(FlinkMessagesBundle.message("main.page.jobManager")),
    JARS(FlinkMessagesBundle.message("main.page.jars"))
  }
}