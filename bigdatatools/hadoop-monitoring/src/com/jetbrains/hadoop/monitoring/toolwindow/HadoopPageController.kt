package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.TabsListener
import com.jetbrains.bigdatatools.common.monitoring.toolwindow.ComponentController
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.ui.JBRunnerTabsBorderless
import com.jetbrains.hadoop.monitoring.settings.HadoopConnectionData
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings
import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle
import javax.swing.JComponent

class HadoopPageController(project: Project, connectionData: HadoopConnectionData) : ComponentController {

  private val tabs = JBRunnerTabsBorderless(project, this)

  init {
    if (connectionData.uri.isNotEmpty()) {

      DriverManager.onDriversInit(project) {
        init(project, connectionData)
      }
    }
  }

  private fun init(project: Project, connectionData: HadoopConnectionData) {
    val clusterInfo = ClusterInfo(project, connectionData)
    val clusterInfoTab = TabInfo(clusterInfo.getComponent())

    clusterInfoTab.setText(HadoopMessagesBundle.message("tab.info"))
    tabs.addTab(clusterInfoTab)
    Disposer.register(this, clusterInfo)

    val nodes = Nodes(project, connectionData)
    val nodesTab = TabInfo(nodes.getComponent())
    nodesTab.setText(HadoopMessagesBundle.message("tab.nodes"))
    tabs.addTab(nodesTab)
    Disposer.register(this, nodes)

    val nodeLabels = NodeLabels(project, connectionData)
    val nodeLabelsTab = TabInfo(nodeLabels.getComponent())
    nodeLabelsTab.setText(HadoopMessagesBundle.message("tab.nodeLabels"))
    tabs.addTab(nodeLabelsTab)
    Disposer.register(this, nodeLabels)

    val applications = Applications(project, connectionData)
    val applicationsTab = TabInfo(applications.getComponent())
    applicationsTab.setText(HadoopMessagesBundle.message("tab.applications"))
    tabs.addTab(applicationsTab)
    Disposer.register(this, applications)

    val tools = Tools(project, connectionData)
    val toolsTab = TabInfo(tools.getComponent())
    toolsTab.setText(HadoopMessagesBundle.message("tab.tools"))
    tabs.addTab(toolsTab)
    Disposer.register(this, tools)

    val config = HadoopSettings.getInstance().configs[connectionData.innerId]
    if (config?.selectedPage != null) {
      val found = tabs.tabs.find { it.text == config.selectedPage }
      found?.let { tabs.select(it, true) }
    }

    tabs.addListener(object : TabsListener {
      override fun selectionChanged(oldSelection: TabInfo?, newSelection: TabInfo?) {
        HadoopSettings.getInstance().setSelectedPage(connectionData.innerId, newSelection?.text ?: "")
      }
    })
  }

  override fun getComponent(): JComponent = tabs

  override fun dispose() {}
}