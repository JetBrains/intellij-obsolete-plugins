package com.intellij.bigdatatools.visualization.inlays.layouts

import com.intellij.bigdatatools.visualization.inlays.NotebookInlayComponentGutter
import com.intellij.bigdatatools.visualization.inlays.components.getHeaderHeight
import com.intellij.bigdatatools.visualization.inlays.pages.InlayPage
import com.intellij.bigdatatools.visualization.inlays.pages.InlayPageContentType
import com.intellij.bigdatatools.visualization.inlays.utils.NotebookInlayUtils
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.TabsListener
import com.intellij.ui.tabs.impl.JBTabsImpl
import com.jetbrains.bigdatatools.common.ui.createButton
import java.awt.Dimension
import javax.swing.JPanel

class NotebookInlayLayoutTabs(project: Project) : NotebookInlayLayout() {

  // In case InlaysConfig.instance.outputLayout == InlaysOutputLayout.TABS, here will be JBTabsImpl.
  override val component: JBTabsImpl = JBTabsImpl(project, this).apply {
    isOpaque = false
  }

  override val title = ""

  override val contentType = InlayPageContentType.MIXED

  override var showToolbar = true

  init {
    updatePreferredSize()
  }

  private fun updatePreferredSize() {
    val tabHeight = component.getHeaderHeight()
    val contentHeight = _pages.maxByOrNull { it.component.preferredSize.height }?.component?.preferredSize?.height ?: 0
    component.preferredSize = Dimension(component.preferredSize.width, tabHeight + contentHeight)
  }

  override fun addContentChangeListener(listener: () -> Unit) {
    component.addListener(object : TabsListener {
      override fun selectionChanged(oldSelection: TabInfo?, newSelection: TabInfo?) {
        listener()
      }
    })
  }

  override fun dispose() {}

  override fun setPages(inlayPages: List<InlayPage>, clear: Boolean) {

    super.setPages(inlayPages, clear)

    component.apply {
      inlayPages.forEach {
        addTab(createTabInfo(it))
        NotebookInlayUtils.doWhenPreferredSizeSet(it.component, once = true) { updatePreferredSize() }
      }
      //addListener(object : TabsListener {
      //  override fun selectionChanged(oldSelection: TabInfo?, newSelection: TabInfo?) {
      //    fireChangeEvent()
      //  }
      //})
    }
  }

  override fun updateGutter(gutter: NotebookInlayComponentGutter) {
    val target = component.selectedInfo?.component ?: return
    gutter.setToolbarsNumber(listOf(target))
    gutter.toolbarsPanel.revalidate()
    gutter.toolbarsPanel.repaint()
  }

  private fun updateToolbar(tabInfo: TabInfo, inlayPage: InlayPage) {
    val panel = tabInfo.sideComponent as JPanel? ?: JPanel()
    panel.removeAll()

    // todo: recreate toolbar and set correct targetComponent
    for (action in inlayPage.createActions()) {
      panel.add(action.createButton())
    }

    tabInfo.setSideComponent(panel)
  }

  private fun createTabInfo(inlayPage: InlayPage): TabInfo {
    val tabInfo = TabInfo(inlayPage.component)
    tabInfo.setText(inlayPage.title)
    tabInfo.setActions(DefaultActionGroup(), "NotebookInlayTabs")
    updateToolbar(tabInfo, inlayPage)
    return tabInfo
  }
}