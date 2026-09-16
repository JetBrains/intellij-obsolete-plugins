package com.intellij.bigdatatools.visualization.inlays.style

import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

class InlaysConfigurable : SearchableConfigurable {

  companion object {
    const val ID = "Settings.Inlays.Style"
  }

  private var configurationPanel: InlaysConfigurationPanel? = null

  override fun reset() {
    configurationPanel?.reset()
  }

  override fun apply() {
    configurationPanel?.apply()
  }

  override fun isModified() = configurationPanel?.isModified() ?: false

  override fun createComponent(): JComponent {
    val panel = configurationPanel ?: InlaysConfigurationPanel()
    if (configurationPanel == null) {
      configurationPanel = panel
    }
    return panel.getComponent()
  }

  override fun getDisplayName() = VisMessagesBundle.message("configurable.displayName")

  override fun getId() = ID

  override fun disposeUIResources() {
    configurationPanel = null
  }
}