package com.intellij.aiplayground.ui.settings

import com.intellij.CommonBundle
import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.aiplayground.models.utils.AiPlaygroundCoroutine
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.chat.view.panel
import com.intellij.aiplayground.ui.env.showEnvApiKeysDialog
import com.intellij.aiplayground.ui.settings.SmartListModel.Companion.AI_PLAYGROUND_SHOW_IMPORT_KEYS_BANNER_IN_SETTINGS
import com.intellij.aiplayground.ui.utils.ApiKeyVerifier
import com.intellij.ide.IdeCoreBundle
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.CommonActionsPanel
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.intellij.ui.LayeredIcon
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.layout.predicate
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.AbstractListModel
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.SwingConstants
import javax.swing.event.ListSelectionListener

/**
 * Configurable for LLM provider settings using Jetpack Compose
 */
class LlmSettingsConfigurable(private val project: Project) : SearchableConfigurable, Configurable.NoScroll {
  private val myPanel: JComponent by lazy { createPanel() }
  private val coroutineScope = service<AiPlaygroundCoroutine>().coroutineScope.childScope("LlmSettingsConfigurable")

  // Settings view model that contains the actual state
  @TestOnly
  val viewModel: LlmSettingsViewModel = LlmSettingsViewModel(project, coroutineScope)

  override fun getId(): String = "com.intellij.aiplayground.settings.llm"

  override fun getDisplayName(): String = AIPlaygroundUIBundle.message("configurable.ai.playground.display.name")

  override fun createComponent(): JComponent {
    return myPanel
  }

  override fun getPreferredFocusedComponent(): JComponent {
    return myPanel
  }

  private fun createPanel(): JComponent = panel {
    row {
      cell(
        Wrapper(
          InlineBanner(EditorNotificationPanel.Status.Info)
            .setMessage(AIPlaygroundUIBundle.message("notification.settings.api.keys.found.text"))
            .addAction(AIPlaygroundUIBundle.message("notification.settings.api.keys.found.action")) {
              PlaygroundCollector.logBannerImportEnvApiKeysInSettingsClicked()
              val foundKeys = viewModel.newApiKeysNotInCurrentProviders.value
              showEnvApiKeysDialog(project, foundKeys)
              viewModel.update()
            }
            .setCloseAction {
              viewModel.updateShowAPIKeysFoundBanner(false)
              PropertiesComponent.getInstance(project).setValue(AI_PLAYGROUND_SHOW_IMPORT_KEYS_BANNER_IN_SETTINGS, false, true)
              PlaygroundCollector.logBannerImportEnvApiKeysInSettingsCanceled()
            }).apply {
          border = JBUI.Borders.empty(4, 8)
          maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        },
      )

      val predicateFlow = combine(
        viewModel.newApiKeysNotInCurrentProviders,
        viewModel.showAPIKeysFoundBanner
      ) { newApiKeysNotInCurrentProviders, showBanner ->
        newApiKeysNotInCurrentProviders.isNotEmpty() && showBanner
      }

      // Log when the banner becomes visible
      coroutineScope.launch {
        predicateFlow
          .filter { it }
          .take(1)
          .collect { PlaygroundCollector.logBannerImportEnvApiKeysInSettingsShown() }
      }

      visibleIf(
        predicateFlow
          .stateIn(coroutineScope, SharingStarted.Eagerly, false)
          .predicate(coroutineScope) { it }
      )
    }
    row {
      cell(
        Splitter().apply {
          firstComponent = createListPanel()
          secondComponent = createPropertiesPanel()
          proportion = 0.3f
        }
      )
    }
  }

  private val listModel = SmartListModel<LlmProviderInstance> { it.id }

  private fun createListPanel(): JComponent {
    val list = JBList(listModel)
    list.addListSelectionListener(ListSelectionListener { _ -> viewModel.updateSelectedProvider(list.selectedValue) })
    list.cellRenderer = ListCellRenderer { _, value, _, _, _ -> JLabel(value.settings.displayName) }
    coroutineScope.launch {
      viewModel.providers.collectLatest {
        listModel.updateItems(it)
      }
    }
    return ToolbarDecorator.createDecorator(list).setMoveUpAction(null)
      .setRemoveAction(null)
      .setMoveDownAction(null)
      .setAddAction(null)
      .addExtraAction(object : ActionGroup("", true), DumbAware {

        private var providers: List<LlmProvider> = emptyList()

        init {
          coroutineScope.launch {
            viewModel.getNotSelectedProviders().collectLatest {
              providers = it
            }
          }
          getTemplatePresentation().setIcon(LayeredIcon.ADD_WITH_DROPDOWN)
          registerCustomShortcutSet(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD), list)
        }

        override fun getChildren(e: AnActionEvent?): Array<AnAction> {
          return providers.map {
            object : DumbAwareAction(it.displayName) {
              override fun actionPerformed(e: AnActionEvent) {
                viewModel.addProvider(it)
              }
            }
          }.toTypedArray()
        }

      })
      .addExtraAction(object : DumbAwareAction(
        CommonBundle.messagePointer("button.delete"),
        CommonBundle.messagePointer("button.delete"),
        PlatformIcons.DELETE_ICON
      ) {

        override fun actionPerformed(e: AnActionEvent) {
          viewModel.removeProvider(list.selectedValue ?: return)
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun update(e: AnActionEvent) {
          e.presentation.isEnabledAndVisible = list.selectedValue?.provider?.predefined == false
        }

        init {
          getTemplatePresentation().setIcon(PlatformIcons.DELETE_ICON)
          registerCustomShortcutSet(CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.REMOVE), list)
        }

      })
      .createPanel()
  }

  private fun createPropertiesPanel(): JComponent {
    return JPanel().apply {
      layout = BorderLayout()
      coroutineScope.launch {
        viewModel.providerConfigurable.collectLatest { configurable ->
          removeAll()
          val component = configurable?.createComponent() ?: JLabel(IdeCoreBundle.message("message.nothingToShow"), SwingConstants.CENTER)
          add(component, BorderLayout.CENTER)
          revalidate()
        }
      }
    }
  }

  override fun isModified(): Boolean {
    return viewModel.isModified()
  }

  override fun apply() {
    val changedProviders = viewModel.getChangedProviders()

    viewModel.apply()

    service<AiPlaygroundCoroutine>().coroutineScope.launch {
      for (provider in changedProviders) {
        ApiKeyVerifier.verifyApiKey(project, provider.provider, provider.settings,
                                    showSuccessNotification = true)
      }
    }
  }

  override fun reset() {
    viewModel.reset()
  }

  override fun disposeUIResources() {
    coroutineScope.cancel()
    viewModel.dispose()
  }
}

class SmartListModel<T>(
  private val idSupplier: (T) -> Any,
) : AbstractListModel<T>() {
  private val items = mutableListOf<T>()

  /**
   * Updates the list content maintaining selection using provided ID supplier for comparison
   * @return List of indices that were actually changed
   */
  fun updateItems(newItems: List<T>): List<Int> {
    val changes = mutableListOf<Int>()
    val newSize = newItems.size
    val oldSize = items.size

    // First pass - update existing items and mark changes
    val minSize = minOf(oldSize, newSize)
    for (i in 0 until minSize) {
      val oldItem = items[i]
      val newItem = newItems[i]
      if (idSupplier(oldItem) != idSupplier(newItem)) {
        // Different item at this position
        changes.add(i)
        items[i] = newItem
      }
      else if (oldItem != newItem) {
        // Same item (by ID) but different content
        changes.add(i)
        items[i] = newItem
      }
    }

    if (newSize > oldSize) {
      // Add new items
      val added = newItems.subList(oldSize, newSize)
      items.addAll(added)
      changes.addAll(oldSize until newSize)
      fireIntervalAdded(this, oldSize, newSize - 1)
    }
    else if (oldSize > newSize) {
      // Remove extra items
      repeat(oldSize - newSize) {
        items.removeLast()
      }
      fireIntervalRemoved(this, newSize, oldSize - 1)
    }

    // Notify about individual changes within the common size range
    changes.filter { it < minSize }
      .forEach { index ->
        fireContentsChanged(this, index, index)
      }

    return changes
  }

  override fun getSize(): Int = items.size

  override fun getElementAt(index: Int): T = items[index]

  companion object {
    const val AI_PLAYGROUND_SHOW_IMPORT_KEYS_BANNER_IN_SETTINGS: String = "ai.playground.ignore.import.keys.banner.in.settings"
  }
}