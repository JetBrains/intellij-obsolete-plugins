package com.intellij.bigdatatools.notebooks.style

import com.intellij.bigdatatools.coreUi.ui.block
import com.intellij.bigdatatools.notebooks.utils.NoteMessagesBundle
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class GlobalStyleConfigurable : SearchableConfigurable {

  companion object {
    const val ID = "Notebook.Style"

    private val EXTENSION_POINT_NAME: ExtensionPointName<SearchableConfigurable> =
      ExtensionPointName.create("com.intellij.bigdatatools.notebook.style")
  }

  private var providers: List<SearchableConfigurable> = emptyList()

  override fun reset() = providers.forEach { it.reset() }

  override fun apply() = providers.forEach { it.apply() }

  override fun isModified() = providers.firstOrNull { it.isModified } != null

  override fun createComponent(): JComponent {
    providers = EXTENSION_POINT_NAME.extensionList
    return panel {
      providers.forEach {
        val component = it.createComponent()
        if (component != null) {
          group(it.displayName) {
            block(component)
          }
        }
      }
    }
  }

  override fun getDisplayName() = NoteMessagesBundle.message("style.displayName")

  override fun getId() = ID

  override fun disposeUIResources() = Unit

  override fun getHelpTopic() = "big.data.tools.notebook.settings"
}