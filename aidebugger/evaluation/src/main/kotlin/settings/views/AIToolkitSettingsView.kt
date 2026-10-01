package com.intellij.aidebugger.evaluation.settings.views

import com.intellij.CommonBundle
import com.intellij.aidebugger.evaluation.EvaluationCollector
import com.intellij.aidebugger.evaluation.settings.AIToolkitUIBundle
import com.intellij.aidebugger.evaluation.settings.env.showEnvApiKeysDialog
import com.intellij.aidebugger.evaluation.settings.models.ProviderInstance
import com.intellij.aidebugger.evaluation.settings.viewmodels.AIToolkitSettingsViewModel
import com.intellij.aidebugger.evaluation.settings.viewmodels.AIToolkitSettingsViewModel.Companion.AI_TOOLKIT_SHOW_IMPORT_KEYS_BANNER_IN_SETTINGS
import com.intellij.ide.IdeCoreBundle
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
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
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.ui.layout.predicate
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
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
 * Master-detail view matching AI Playground design.
 * Uses Splitter, ToolbarDecorator, and Configurable pattern.
 */
class AIToolkitSettingsView(
    project: Project,
    parentScope: CoroutineScope,
    private val viewModel: AIToolkitSettingsViewModel
) : JPanel(BorderLayout()), Disposable {

    private val coroutineScope = parentScope.childScope("AIToolkitSettingsView")
    private val listModel = SmartListModel<ProviderInstance> { it.id }

    init {
        add(
            panel {
                row {
                    cell(
                        Wrapper(
                            InlineBanner(EditorNotificationPanel.Status.Info)
                                .setMessage(AIToolkitUIBundle.message("notification.settings.api.keys.found.text"))
                                .addAction(AIToolkitUIBundle.message("notification.settings.api.keys.found.action")) {
                                    EvaluationCollector.reportSettingsImportKeysBannerClicked(project)
                                    val foundKeys = viewModel.newApiKeysNotInCurrentProviders.value
                                    val success = showEnvApiKeysDialog(project, foundKeys)
                                    if (success) {
                                        EvaluationCollector.reportSettingsImportKeysBannerClickedAndSuccess(project)
                                    } else {
                                        EvaluationCollector.reportSettingsImportKeysBannerClickedAndCancelled(project)
                                    }
                                    viewModel.update()
                                }
                                .setCloseAction {
                                    EvaluationCollector.reportSettingsImportKeysBannerHidden(project)
                                    viewModel.updateShowAPIKeysFoundBanner(false)
                                    PropertiesComponent.getInstance(project)
                                        .setValue(AI_TOOLKIT_SHOW_IMPORT_KEYS_BANNER_IN_SETTINGS, false, true)
                                }).apply {
                            border = JBUI.Borders.empty(4, 8)
                            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
                        },
                    )
                        .align(Align.FILL)
                        .customize(UnscaledGaps(top = 8, bottom = 8, left = 4, right = 4))
                        .resizableColumn()

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
                            .collect { EvaluationCollector.reportSettingsImportKeysBannerShown(project) }
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
                        .align(Align.FILL)
                        .resizableColumn()
                }
                    .resizableRow()
            }
        )
    }

    private fun createListPanel(): JComponent {
        val list = JBList(listModel)
        list.addListSelectionListener(ListSelectionListener { _ ->
            if (!list.valueIsAdjusting) {
                viewModel.selectInstance(list.selectedValue)
            }
        })
        list.cellRenderer = ListCellRenderer { _, value, _, _, _ ->
            JLabel(value.name)
        }

        // Observe instances and update list
        coroutineScope.launch {
            viewModel.instances.collectLatest { instances ->
                listModel.updateItems(instances)
            }
        }

        // Observe selection changes from ViewModel and update list selection
        coroutineScope.launch {
            viewModel.selectedInstance.collectLatest { selected ->
                val instances = viewModel.instances.value
                if (selected != null) {
                    val index = instances.indexOfFirst { it.id == selected.id }
                    if (index >= 0 && list.selectedIndex != index) {
                        list.selectedIndex = index
                    }
                } else {
                    // Clear selection when ViewModel has no selection
                    list.clearSelection()
                }
            }
        }

        return ToolbarDecorator.createDecorator(list)
            .setMoveUpAction(null)
            .setMoveDownAction(null)
            .setAddAction(null)
            .setRemoveAction(null)
            .addExtraAction(object : ActionGroup("", true), DumbAware {
                init {
                    getTemplatePresentation().icon = LayeredIcon.ADD_WITH_DROPDOWN
                    registerCustomShortcutSet(
                        CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD),
                        list
                    )
                }

                override fun getChildren(e: AnActionEvent?): Array<AnAction> {
                    val existingTypes = viewModel.instances.value.map { it.providerType }.toSet()
                    return viewModel.availableProviderTypes.map { type ->
                        object : DumbAwareAction(type.displayName) {
                            override fun actionPerformed(e: AnActionEvent) {
                                viewModel.addInstance(type)
                            }

                            override fun update(e: AnActionEvent) {
                                e.presentation.isEnabled = type !in existingTypes
                            }

                            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
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
                    viewModel.removeSelectedInstance()
                }

                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabledAndVisible = list.selectedValue != null
                }

                init {
                    getTemplatePresentation().icon = PlatformIcons.DELETE_ICON
                    registerCustomShortcutSet(
                        CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.REMOVE),
                        list
                    )
                }
            })
            .createPanel()
    }

    private fun createPropertiesPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            coroutineScope.launch {
                // Only recreate view when instance ID changes, not when properties change
                viewModel.selectedInstance
                    .distinctUntilChangedBy { it?.id }
                    .collectLatest { instance ->
                        removeAll()
                        val component = if (instance != null) {
                            AIToolkitProviderInstanceView(coroutineScope, viewModel, instance).createComponent()
                        } else {
                            JLabel(IdeCoreBundle.message("message.nothingToShow"), SwingConstants.CENTER)
                        }
                        add(component, BorderLayout.CENTER)
                        revalidate()
                        repaint()
                    }
            }
        }
    }

    override fun dispose() {
        coroutineScope.cancel()
    }
}

/**
 * Smart list model that maintains selection by ID (from AI Playground)
 */
class SmartListModel<T>(
    private val idSupplier: (T) -> Any,
) : AbstractListModel<T>() {
    private val items = mutableListOf<T>()

    fun updateItems(newItems: List<T>): List<Int> {
        val changes = mutableListOf<Int>()
        val newSize = newItems.size
        val oldSize = items.size

        val minSize = minOf(oldSize, newSize)
        for (i in 0 until minSize) {
            val oldItem = items[i]
            val newItem = newItems[i]
            if (idSupplier(oldItem) != idSupplier(newItem)) {
                changes.add(i)
                items[i] = newItem
            } else if (oldItem != newItem) {
                changes.add(i)
                items[i] = newItem
            }
        }

        if (newSize > oldSize) {
            val added = newItems.subList(oldSize, newSize)
            items.addAll(added)
            changes.addAll(oldSize until newSize)
            fireIntervalAdded(this, oldSize, newSize - 1)
        } else if (oldSize > newSize) {
            repeat(oldSize - newSize) {
                items.removeLast()
            }
            fireIntervalRemoved(this, newSize, oldSize - 1)
        }

        changes.filter { it < minSize }
            .forEach { index ->
                fireContentsChanged(this, index, index)
            }

        return changes
    }

    override fun getSize(): Int = items.size

    override fun getElementAt(index: Int): T = items[index]
}
