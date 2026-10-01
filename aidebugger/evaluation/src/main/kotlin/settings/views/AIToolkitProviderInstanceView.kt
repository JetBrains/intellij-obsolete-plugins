
package com.intellij.aidebugger.evaluation.settings.views

import com.intellij.aidebugger.evaluation.settings.AIToolkitUIBundle
import com.intellij.aidebugger.evaluation.settings.models.LlmModel
import com.intellij.aidebugger.evaluation.settings.models.ProviderInstance
import com.intellij.aidebugger.evaluation.settings.viewmodels.AIToolkitSettingsViewModel
import com.intellij.aidebugger.evaluation.settings.viewmodels.AIToolkitSettingsViewModel.ConnectionState
import com.intellij.icons.AllIcons
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.CheckBoxList
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class AIToolkitProviderInstanceView(
    private val coroutineScope: CoroutineScope,
    private val viewModel: AIToolkitSettingsViewModel,
    private val instance: ProviderInstance
) {
    private var filterText = ""
    private var isUpdatingFromViewModel = false

    fun createComponent(): JComponent {
        val mainPanel = panel {
            row(AIToolkitUIBundle.message("provider.type")) {
                label(instance.providerType.displayName)
            }

            row(AIToolkitUIBundle.message("name")) {
                textField()
                    .align(AlignX.FILL)
                    .applyToComponent {
                        text = instance.name
                        document.addDocumentListener(object : DocumentListener {
                            override fun insertUpdate(e: DocumentEvent?) = updateName()
                            override fun removeUpdate(e: DocumentEvent?) = updateName()
                            override fun changedUpdate(e: DocumentEvent?) = updateName()

                            private fun updateName() {
                                if (!isUpdatingFromViewModel) {
                                    viewModel.selectedInstance.value?.let { current ->
                                        if (current.id == instance.id) {
                                            viewModel.updateSelectedInstance(current.copy(name = text))
                                        }
                                    }
                                }
                            }
                        })
                    }
            }

            row(AIToolkitUIBundle.message("api.key")) {
                passwordField()
                    .align(AlignX.FILL)
                    .applyToComponent {
                        text = instance.apiKey
                        document.addDocumentListener(object : DocumentListener {
                            override fun insertUpdate(e: DocumentEvent?) = updateApiKey()
                            override fun removeUpdate(e: DocumentEvent?) = updateApiKey()
                            override fun changedUpdate(e: DocumentEvent?) = updateApiKey()

                            private fun updateApiKey() {
                                if (!isUpdatingFromViewModel) {
                                    viewModel.selectedInstance.value?.let { current ->
                                        if (current.id == instance.id) {
                                            viewModel.updateSelectedInstance(current.copy(apiKey = String(password)))
                                        }
                                    }
                                }
                            }
                        })
                    }
            }

            if (instance.providerType.supportsBaseUrl) {
                row(AIToolkitUIBundle.message("base.url")) {
                    textField()
                        .align(AlignX.FILL)
                        .applyToComponent {
                            text = instance.baseUrl
                            document.addDocumentListener(object : DocumentListener {
                                override fun insertUpdate(e: DocumentEvent?) = updateBaseUrl()
                                override fun removeUpdate(e: DocumentEvent?) = updateBaseUrl()
                                override fun changedUpdate(e: DocumentEvent?) = updateBaseUrl()

                                private fun updateBaseUrl() {
                                    if (!isUpdatingFromViewModel) {
                                        viewModel.selectedInstance.value?.let { current ->
                                            if (current.id == instance.id) {
                                                viewModel.updateSelectedInstance(current.copy(baseUrl = text))
                                            }
                                        }
                                    }
                                }
                            })
                        }
                }
            }

            row {
                cell()

                link(AIToolkitUIBundle.message("link.label.test.connection")) {
                    viewModel.testConnection()
                }.component

                val statusLabel = icon(AllIcons.Empty).component

                statusLabel.apply {
                    coroutineScope.launch {
                        viewModel.connectionState.collectLatest { states ->
                            val state = states[instance.id] ?: ConnectionState.UNKNOWN
                            SwingUtilities.invokeLater {
                                isVisible = state != ConnectionState.UNKNOWN

                                when (state) {
                                    ConnectionState.UNKNOWN -> {
                                        text = ""
                                        icon = AllIcons.Empty
                                    }
                                    ConnectionState.CONNECTING -> {
                                        text = AIToolkitUIBundle.message("connecting")
                                        icon = AnimatedIcon.Default()
                                    }
                                    ConnectionState.CONNECTED -> {
                                        text = AIToolkitUIBundle.message("connected")
                                        icon = AllIcons.General.GreenCheckmark
                                        viewModel.fetchModels()
                                    }
                                    ConnectionState.FAILED -> {
                                        text = AIToolkitUIBundle.message("failed.to.connect")
                                        icon = AllIcons.General.Warning
                                    }
                                }
                            }
                        }
                    }
                }

                val modelsLabel = icon(AllIcons.Empty).component

                modelsLabel.apply {
                    coroutineScope.launch {
                        viewModel.modelsLoading.collectLatest { loadingStates ->
                            val isLoading = loadingStates[instance.id] ?: false
                            SwingUtilities.invokeLater {
                                isVisible = isLoading
                                if (isLoading) {
                                    text = AIToolkitUIBundle.message("loading.models")
                                    icon = AnimatedIcon.Default()
                                } else {
                                    text = ""
                                    icon = AllIcons.Empty
                                }
                            }
                        }
                    }
                }
            }

            separator()

            row {
                checkBox(AIToolkitUIBundle.message("enable.all.models"))
                    .applyToComponent {
                        addActionListener {
                            if (!isUpdatingFromViewModel) {
                                val modelIds = viewModel.availableModels.value[instance.id]?.map { it.id } ?: emptyList()
                                if (isSelected) {
                                    viewModel.enableModels(modelIds)
                                } else {
                                    viewModel.disableModels(modelIds)
                                }
                            }
                        }

                        coroutineScope.launch {
                            combine(
                                viewModel.modelsLoading,
                                viewModel.availableModels,
                                viewModel.selectedInstance
                            ) { loading, models, selected ->
                                Triple(loading[instance.id] == true, models[instance.id], selected)
                            }.collect { (isLoading, models, selected) ->
                                SwingUtilities.invokeLater {
                                    if (selected?.id == instance.id) {
                                        isEnabled = !isLoading && !models.isNullOrEmpty()
                                        isUpdatingFromViewModel = true
                                        isSelected = models?.all { selected.isModelEnabled(it.id) } == true
                                        isUpdatingFromViewModel = false
                                    }
                                }
                            }
                        }
                    }
            }

            row(AIToolkitUIBundle.message("models.label")) {}

            row {
                cell(createModelsPanel())
                    .align(AlignX.FILL)
                    .align(AlignY.FILL)
            }.resizableRow()
        }

        return JBScrollPane(mainPanel).apply {
            horizontalScrollBarPolicy = javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            border = null
        }
    }

    private fun createModelsPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        val checkBoxList = CheckBoxList<LlmModel>()

        val searchField = SearchTextField().apply {
            addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) = updateFilter()
                override fun removeUpdate(e: DocumentEvent?) = updateFilter()
                override fun changedUpdate(e: DocumentEvent?) = updateFilter()

                private fun updateFilter() {
                    filterText = text
                    updateModelList(checkBoxList)
                }
            })
        }

        checkBoxList.setCheckBoxListListener { index, selected ->
            val model = checkBoxList.getItemAt(index) ?: return@setCheckBoxListListener
            if (selected) {
                viewModel.enableModels(listOf(model.id))
            } else {
                viewModel.disableModels(listOf(model.id))
            }
        }

        panel.add(searchField, BorderLayout.NORTH)
        panel.add(JBScrollPane(checkBoxList), BorderLayout.CENTER)

        coroutineScope.launch {
            combine(
                viewModel.availableModels,
                viewModel.selectedInstance,
                viewModel.modelsLoading
            ) { models, selected, loading ->
                Triple(models, selected, loading)
            }.collectLatest { (_, selected, loading) ->
                if (selected?.id == instance.id) {
                    val isLoading = loading[instance.id] ?: false
                    SwingUtilities.invokeLater {
                        checkBoxList.isEnabled = !isLoading
                        searchField.isEnabled = !isLoading
                        isUpdatingFromViewModel = true
                        updateModelList(checkBoxList)
                        isUpdatingFromViewModel = false
                    }
                }
            }
        }

        return panel
    }

    private fun updateModelList(checkBoxList: CheckBoxList<LlmModel>) {
        val currentInstance = viewModel.selectedInstance.value ?: return
        if (currentInstance.id != instance.id) return

        val models = viewModel.availableModels.value[instance.id] ?: emptyList()
        val filtered = if (filterText.isBlank()) {
            models
        } else {
            models.filter {
                it.id.contains(filterText, ignoreCase = true) ||
                        it.displayName.contains(filterText, ignoreCase = true)
            }
        }

        checkBoxList.clear()
        filtered.forEach { model ->
            checkBoxList.addItem(model, model.displayName, currentInstance.isModelEnabled(model.id))
        }
    }
}