package com.intellij.aidebugger.evaluation.views.dialogs

import com.intellij.aidebugger.common.onboarding.AnchorBus
import com.intellij.aidebugger.common.onboarding.OnboardingAnchorKeys
import com.intellij.aidebugger.common.onboarding.OnboardingRuntimeFlags
import com.intellij.aidebugger.evaluation.EvaluationBundle
import com.intellij.aidebugger.evaluation.EvaluationCollector
import com.intellij.aidebugger.evaluation.models.entities.ConfigInfo
import com.intellij.aidebugger.evaluation.models.entities.EvaluatorConfig
import com.intellij.aidebugger.evaluation.models.repositories.EvalConfigsRepository
import com.intellij.aidebugger.evaluation.settings.AIToolkitUIBundle
import com.intellij.aidebugger.evaluation.settings.env.ApiKeysInEnvVariablesService
import com.intellij.aidebugger.evaluation.settings.env.showSettingsAndEnvApiKeysDialog
import com.intellij.aidebugger.evaluation.settings.models.LlmModel
import com.intellij.aidebugger.evaluation.settings.models.ProviderInstance
import com.intellij.aidebugger.evaluation.viewModels.EvalConfigPopupViewModel
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.IconUtil
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.max
import kotlin.math.min

private data class InitialFields(
    val name: @NlsSafe String,
    val description: @NlsSafe String,
    val datasetLabel: @NlsSafe String,
    val modelName: @NlsSafe String,
)

/**
 * Shows a dialog to create or edit an Evaluation configuration.
 */
fun showEvalConfigPopup(
    project: Project,
    viewModel: EvalConfigPopupViewModel,
    existing: ConfigInfo?,
    onSaved: ((String) -> Unit)? = null
) {
    val ideFrame = com.intellij.openapi.wm.WindowManager.getInstance().getFrame(project)
    val ideSize = ideFrame?.size ?: Dimension(1920, 1080)
    val targetWidth = (ideSize.width * 0.40).toInt().coerceIn(600, 1200)
    val targetHeight = (ideSize.height * 0.70).toInt().coerceIn(500, 1000)

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val uiDisposable = Disposer.newDisposable()
    viewModel.loadConfig(existing)

    val paramRows: MutableList<Pair<JBTextField, JBTextField>> = mutableListOf()
    var closeDialogRef: () -> Unit = {}

    fun lockHeight(c: JComponent) {
        val h = c.preferredSize.height
        c.minimumSize = Dimension(0, h)
        c.preferredSize = Dimension(c.preferredSize.width, h)
        c.maximumSize = Dimension(Int.MAX_VALUE, h)
    }

    fun createSquareIconButton(baseIcon: Icon, targetSizePx: Int = 14): JButton {
        val s = JBUI.scale(targetSizePx)
        val baseW = max(1, baseIcon.iconWidth)
        val scale = s.toFloat() / baseW.toFloat()
        val scaledIcon = IconUtil.scale(baseIcon, null, scale)
        return JButton(scaledIcon).apply {
            isContentAreaFilled = false
            isBorderPainted = false
            isFocusPainted = false
            margin = JBUI.emptyInsets()
            preferredSize = Dimension(s, s)
            minimumSize = Dimension(s, s)
            maximumSize = Dimension(s, s)
        }
    }

    fun createFormPanel(): Triple<JPanel, (String, JComponent) -> Unit, (JComponent, JComponent) -> Unit> {
        val panel = JPanel(GridBagLayout())
        val grid = GridBag().setDefaultInsets(JBUI.insets(4))
        val addRowFn = { label: String, comp: JComponent ->
            val text = label.trim()
            val display = if (text.endsWith(":")) text else "$text:"
            val l = JLabel(display)
            panel.add(l, grid.nextLine().next().weightx(0.0).anchor(GridBagConstraints.WEST).insets(
                JBUI.insets(
                    0,
                    4,
                    0,
                    8
                )
            ))
            panel.add(comp, grid.next().weightx(1.0).fillCellHorizontally())
        }
        val addCustomRow = { left: JComponent, right: JComponent ->
            panel.add(left, grid.nextLine().next().weightx(0.0).anchor(GridBagConstraints.WEST).insets(
                JBUI.insets(
                    0,
                    4,
                    0,
                    8
                )
            ))
            panel.add(right, grid.next().weightx(1.0).fillCellHorizontally())
        }
        return Triple(panel, addRowFn, addCustomRow)
    }

    fun buildInitialFields(): InitialFields =
        InitialFields(
            name = viewModel.name.value,
            description = viewModel.description.value,
            datasetLabel = viewModel.selectedDataset.value,
            modelName = viewModel.modelName.value
        )

    fun buildPromptTemplateTooltipText(template: String): @Nls String? {
        val vars = Regex("\\{([\\p{L}\\p{N}_]+)\\}")
            .findAll(template)
            .map { it.groupValues[1] }
            .toSet()

        if (vars.isEmpty()) return null

        val unknownVars = vars.filterNot { it in EvaluatorConfig.ALLOWED_PROMPT_VARS }.sorted()
        if (unknownVars.isEmpty()) return null

        val allowed = EvaluatorConfig.ALLOWED_PROMPT_VARS.sorted().joinToString(", ") { "{$it}" }
        val unknown = unknownVars.joinToString(", ") { "{$it}" }

        return EvaluationBundle.message(
            "eval.config.prompt.unknownFields.tooltip",
            unknown,
            allowed
        )
    }

    fun isLlmScoreDefinitionMissing(prompt: String): Boolean {
        val hasScore = Regex("(?i)\"score\"\\s*:|\\bscore\\s*:").containsMatchIn(prompt)
        val hasExplanation = Regex("(?i)\"explanation\"\\s*:|\\bexplanation\\s*:").containsMatchIn(prompt)
        return !(hasScore && hasExplanation)
    }

    fun buildLlmScoreDefinitionTooltipText(): @Nls String {
        val example = """
            ```json
            {{ "score": 1, "explanation": "Matches the reference aside from casing/spacing." }}
            ```
        """.trimIndent()

        val escaped = example
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

        return EvaluationBundle.message(
            "eval.config.prompt.missingScoreDefinition.tooltip",
            escaped
        )
    }

    val initialFields = buildInitialFields()

    val tfName = JBTextField(initialFields.name)
    val defaultNameBorder = tfName.border
    val taDesc = JBTextArea(initialFields.description, 3, 20)
    val descScroll = JBScrollPane(taDesc)

    var isUpdatingDataset = false
    val cbDataset = ComboBox(viewModel.availableDatasets.value.toTypedArray()).apply {
        selectedItem = initialFields.datasetLabel
    }

    var isUpdatingRunConfig = false
    val cbRunConfig = object : ComboBox<String>(viewModel.availableRunConfigs.value.toTypedArray()) {
        override fun getPreferredSize(): Dimension {
            val size = super.getPreferredSize()
            val maxWidth = (targetWidth * 0.6).toInt()
            val minWidth = JBUI.scale(150)
            val width = size.width.coerceIn(minWidth, maxWidth)
            return Dimension(width, size.height)
        }
    }.apply {
        isEnabled = viewModel.availableRunConfigs.value.isNotEmpty()
        selectedItem = viewModel.runConfigName.value
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JComponent
                component.toolTipText = value?.toString()
                return component
            }
        }
    }

    var isUpdatingProvider = false
    val cbProvider = ComboBox(viewModel.availableProviders.value.toTypedArray()).apply {
        isEnabled = viewModel.availableProviders.value.isNotEmpty()
        selectedItem = viewModel.selectedProvider.value
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                label.text = (value as? ProviderInstance)?.getDisplayName() ?: ""
                return label
            }
        }
    }

    var isUpdatingModel = false
    val cbModel = ComboBox<LlmModel>().apply {
        isEditable = false
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                label.text = (value as? LlmModel)?.displayName ?: ""
                return label
            }
        }
        addActionListener {
            if (!isUpdatingModel) {
                viewModel.setSelectedModel(selectedItem as? LlmModel)
            }
        }
    }

    viewModel.availableDatasets
        .onEach { list ->
            isUpdatingDataset = true
            val current = cbDataset.selectedItem
            cbDataset.model = DefaultComboBoxModel(list.toTypedArray())
            if (current != null && list.contains(current)) {
                cbDataset.selectedItem = current
            }
            isUpdatingDataset = false
        }
        .launchIn(scope)

    viewModel.selectedDataset
        .onEach { label ->
            if (label != cbDataset.selectedItem) {
                isUpdatingDataset = true
                cbDataset.selectedItem = label
                isUpdatingDataset = false
            }
        }
        .launchIn(scope)

    viewModel.availableRunConfigs
        .onEach { list ->
            isUpdatingRunConfig = true
            val current = cbRunConfig.selectedItem
            cbRunConfig.model = DefaultComboBoxModel(list.toTypedArray())
            cbRunConfig.isEnabled = list.isNotEmpty()
            if (current != null && list.contains(current)) {
                cbRunConfig.selectedItem = current
            }
            isUpdatingRunConfig = false
        }
        .launchIn(scope)

    viewModel.runConfigName
        .onEach { name ->
            if (name != cbRunConfig.selectedItem) {
                isUpdatingRunConfig = true
                cbRunConfig.selectedItem = name
                isUpdatingRunConfig = false
            }
        }
        .launchIn(scope)

    viewModel.selectedProvider
        .onEach { provider ->
            if (provider != cbProvider.selectedItem) {
                isUpdatingProvider = true
                cbProvider.selectedItem = provider
                isUpdatingProvider = false
            }
        }
        .launchIn(scope)

    viewModel.availableModels
        .onEach { models ->
            isUpdatingModel = true
            cbModel.removeAllItems()
            models.forEach { cbModel.addItem(it) }
            isUpdatingModel = false
        }
        .launchIn(scope)

    viewModel.selectedModel
        .onEach { model ->
            isUpdatingModel = true
            cbModel.selectedItem = model
            isUpdatingModel = false
        }
        .launchIn(scope)

    kotlinx.coroutines.flow.combine(
        viewModel.availableModels,
        viewModel.isLoadingModels
    ) { models, loading ->
        models.isNotEmpty() && !loading
    }.onEach { enabled ->
        cbModel.isEnabled = enabled
    }.launchIn(scope)
    lockHeight(tfName)
    lockHeight(cbDataset)
    lockHeight(descScroll)
    lockHeight(cbRunConfig)
    lockHeight(cbProvider)
    lockHeight(cbModel)

    viewModel.nameValidationError
        .onEach { error ->
            val (border, tooltip) = when (error) {
                null -> defaultNameBorder to null
                EvalConfigPopupViewModel.NameValidationError.INVALID_CHARACTERS ->
                    JBUI.Borders.customLine(JBColor.RED) to
                            EvaluationBundle.message("eval.config.name.invalidChars.tooltip")
                EvalConfigPopupViewModel.NameValidationError.ALREADY_EXISTS ->
                    JBUI.Borders.customLine(JBColor.RED) to
                            EvaluationBundle.message("eval.config.name.alreadyExists.tooltip")
            }
            tfName.border = border
            tfName.toolTipText = tooltip
        }
        .launchIn(scope)

    // General section
    val (generalForm, addGeneralRow, _) = createFormPanel()
    addGeneralRow(EvaluationBundle.message("eval.config.row.name"), tfName)
    addGeneralRow(EvaluationBundle.message("eval.config.row.input.dataset"), cbDataset)

    // Evaluators section: multiple evaluators support
    val evaluatorsPanel = JPanel(GridBagLayout())
    var rebuildEvaluatorsUIRef: () -> Unit = {}

    fun buildEvaluatorRowPanel(idx: Int, evaluator: EvaluatorConfig): JPanel {
        val rowPanel = JPanel(BorderLayout(4, 0))
        // lightweight separator border for multiple evaluators
        if (idx > 0) {
            rowPanel.border = JBUI.Borders.merge(
                JBUI.Borders.customLineTop(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
                JBUI.Borders.empty(8, 0, 0, 0),
                true
            )
        }
        val contentPanel = JPanel(GridBagLayout())
        val grid = GridBag().setDefaultInsets(JBUI.insets(4))

        val tfName = JBTextField(evaluator.name)
        val cbType = ComboBox(arrayOf("llm judge", "regexp")).apply {
            selectedItem = evaluator.type
        }

        val firstRowPanel = JPanel(GridBagLayout())
        val firstRowGrid = GridBag().setDefaultInsets(JBUI.insets(4))

        firstRowPanel.add(JLabel(EvaluationBundle.message("eval.config.evaluator.name.label")), firstRowGrid.nextLine().next().anchor(GridBagConstraints.WEST).insets(JBUI.insets(0, 4, 0, 8)))
        firstRowPanel.add(tfName, firstRowGrid.next().weightx(0.4).fillCellHorizontally())

        firstRowPanel.add(JLabel(EvaluationBundle.message("eval.config.evaluator.type.label")), firstRowGrid.next().anchor(GridBagConstraints.WEST).insets(JBUI.insets(0, 8, 0, 8)))
        firstRowPanel.add(cbType, firstRowGrid.next().weightx(0.4).fillCellHorizontally())

        // Add delete button on the same row (only if more than 1 evaluator)
        if (viewModel.evaluators.value.size > 1) {
            val btnRemove = createSquareIconButton(AllIcons.Actions.GC)
            btnRemove.toolTipText = EvaluationBundle.message("eval.config.evaluator.remove.tooltip")
            btnRemove.addActionListener {
                viewModel.removeEvaluator(idx)
                rebuildEvaluatorsUIRef()
            }
            firstRowPanel.add(btnRemove, firstRowGrid.next().anchor(GridBagConstraints.WEST).insets(JBUI.insets(0, 8, 0, 0)))
        }

        contentPanel.add(firstRowPanel, grid.nextLine().next().weightx(1.0).fillCellHorizontally())

        val tfPattern = JBTextField(evaluator.pattern ?: ".*{outputExpected}.*")
        val defaultPatternBorder = tfPattern.border
        val patternRow = JPanel(GridBagLayout())
        val patGrid = GridBag().setDefaultInsets(JBUI.insets(4))
        patternRow.add(JLabel(EvaluationBundle.message("eval.config.evaluator.pattern.label")), patGrid.nextLine().next().anchor(GridBagConstraints.WEST).insets(JBUI.insets(0, 4, 0, 8)))
        patternRow.add(tfPattern, patGrid.next().weightx(1.0).fillCellHorizontally())
        patternRow.isVisible = evaluator.type == "regexp"

        val patternConstraints = grid.nextLine().next().weightx(1.0).fillCellHorizontally()
        patternConstraints.gridwidth = GridBagConstraints.REMAINDER
        contentPanel.add(patternRow, patternConstraints)

        val taPromptForEval = JBTextArea(evaluator.prompt ?: "", 8, 40).apply {
            lineWrap = false
            wrapStyleWord = false
        }
        ToolTipManager.sharedInstance().registerComponent(taPromptForEval)
        var activeEditorDoc: com.intellij.openapi.editor.Document? = null
        var isSyncing = false

        val linkOpenEditor = ActionLink(EvaluationBundle.message("eval.config.evaluator.open.in.editor")) {
            // Sync description and params which are not auto-synced by listeners
            viewModel.setDescription(taDesc.text)

            val params = paramRows.mapNotNull { (kField, vField) ->
                val k = kField.text.trim()
                val v = vField.text
                if (k.isNotEmpty()) k to v else null
            }
            viewModel.setModelParams(params)

            // Validate and Save
            if (!viewModel.isValid.value) return@ActionLink

            val savedName = viewModel.save()
            if (savedName.isEmpty()) return@ActionLink
            onSaved?.invoke(savedName)

            val currentPrompt = taPromptForEval.text
            val evaluatorName = evaluator.name

            openPromptInEditor(project, viewModel.configsRepository, savedName, evaluatorName, currentPrompt)

            closeDialogRef()
        }

        val promptScrollForEval = JBScrollPane(taPromptForEval).apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            val prefH = JBUI.scale(180)
            val minH = JBUI.scale(120)
            preferredSize = Dimension(JBUI.scale(620), prefH)
            minimumSize = Dimension(0, minH)
        }
        val defaultPromptBorder = promptScrollForEval.border
        val promptRow = JPanel(BorderLayout()).apply {
            val header = JPanel(BorderLayout())
            header.add(JLabel(EvaluationBundle.message("eval.config.evaluator.prompt.label")), BorderLayout.WEST)
            header.add(linkOpenEditor, BorderLayout.EAST)

            add(header, BorderLayout.NORTH)
            add(promptScrollForEval, BorderLayout.CENTER)
            border = JBUI.Borders.empty(4, 4, 0, 0)
        }
        promptRow.isVisible = evaluator.type == "llm judge"

        val promptConstraints = grid.nextLine().next().weightx(1.0).fillCellHorizontally()
        promptConstraints.gridwidth = GridBagConstraints.REMAINDER
        contentPanel.add(promptRow, promptConstraints)

        fun updateValidation() {
            val isValid = viewModel.evaluatorValidation.value[idx] ?: true
            when (evaluator.type) {
                "regexp" -> {
                    tfPattern.border = if (isValid) defaultPatternBorder else JBUI.Borders.customLine(JBColor.RED)
                    tfPattern.toolTipText = if (isValid) {
                        null
                    } else {
                        buildPromptTemplateTooltipText(tfPattern.text)
                            ?: EvaluationBundle.message(
                                "eval.config.prompt.invalidTemplateVars.tooltip",
                                EvaluatorConfig.ALLOWED_PROMPT_VARS.sorted().joinToString(", ") { "{$it}" }
                            )
                    }
                }

                "llm judge" -> {
                    promptScrollForEval.border = if (isValid) defaultPromptBorder else JBUI.Borders.customLine(JBColor.RED)
                    val tooltip = if (isValid) {
                        null
                    } else {
                        buildPromptTemplateTooltipText(taPromptForEval.text)
                            ?: if (isLlmScoreDefinitionMissing(taPromptForEval.text)) {
                                buildLlmScoreDefinitionTooltipText()
                            } else {
                                EvaluationBundle.message(
                                    "eval.config.prompt.invalidTemplateVars.tooltip",
                                    EvaluatorConfig.ALLOWED_PROMPT_VARS.sorted().joinToString(", ") { "{$it}" }
                                )
                            }
                    }

                    promptScrollForEval.toolTipText = tooltip
                    taPromptForEval.toolTipText = tooltip
                }
            }
        }

        tfName.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateEval()
            override fun removeUpdate(e: DocumentEvent?) = updateEval()
            override fun changedUpdate(e: DocumentEvent?) = updateEval()
            fun updateEval() {
                val current = viewModel.evaluators.value.getOrNull(idx) ?: return
                viewModel.updateEvaluator(idx, current.copy(name = tfName.text))
            }
        })

        cbType.addActionListener {
            val newType = cbType.selectedItem as String
            patternRow.isVisible = newType == "regexp"
            promptRow.isVisible = newType == "llm judge"
            contentPanel.revalidate()
            contentPanel.repaint()

            val current = viewModel.evaluators.value.getOrNull(idx) ?: return@addActionListener
            viewModel.updateEvaluator(idx, current.copy(type = newType))
            updateValidation()
            rebuildEvaluatorsUIRef()
        }

        tfPattern.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateEval()
            override fun removeUpdate(e: DocumentEvent?) = updateEval()
            override fun changedUpdate(e: DocumentEvent?) = updateEval()
            fun updateEval() {
                val current = viewModel.evaluators.value.getOrNull(idx) ?: return
                viewModel.updateEvaluator(idx, current.copy(pattern = tfPattern.text))
                updateValidation()
            }
        })

        taPromptForEval.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateEval()
            override fun removeUpdate(e: DocumentEvent?) = updateEval()
            override fun changedUpdate(e: DocumentEvent?) = updateEval()
            fun updateEval() {
                val current = viewModel.evaluators.value.getOrNull(idx) ?: return
                viewModel.updateEvaluator(idx, current.copy(prompt = taPromptForEval.text))
                updateValidation()

                if (isSyncing) return

                val doc = activeEditorDoc
                if (doc != null && doc.text != taPromptForEval.text) {
                    isSyncing = true
                    try {
                        ApplicationManager.getApplication().runWriteAction {
                            doc.setText(taPromptForEval.text)
                        }
                    } finally {
                        isSyncing = false
                    }
                }
            }
        })

        updateValidation()

        rowPanel.add(contentPanel, BorderLayout.CENTER)

        return rowPanel
    }

    fun rebuildEvaluatorsUI() {
        evaluatorsPanel.removeAll()
        val grid = GridBag().setDefaultInsets(JBUI.insets(4))
        viewModel.evaluators.value.forEachIndexed { idx, evaluator ->
            val rowPanel = buildEvaluatorRowPanel(idx, evaluator)
            evaluatorsPanel.add(rowPanel, grid.nextLine().next().weightx(1.0).fillCellHorizontally())
        }
        evaluatorsPanel.revalidate()
        evaluatorsPanel.repaint()
    }

    rebuildEvaluatorsUIRef = { rebuildEvaluatorsUI() }

    val btnAddEvaluator = JButton(EvaluationBundle.message("eval.config.evaluator.add")).apply {
        toolTipText = EvaluationBundle.message("eval.config.evaluator.add.tooltip")
        addActionListener {
            viewModel.addEvaluator()
            rebuildEvaluatorsUI()
        }
    }
    val addEvaluatorPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
        border = JBUI.Borders.empty(4, 4, 0, 0)
        add(btnAddEvaluator)
    }
    val evaluatorsWrapper = JPanel(BorderLayout()).apply {
        add(evaluatorsPanel, BorderLayout.CENTER)
        add(addEvaluatorPanel, BorderLayout.SOUTH)
    }

    // Model section: provider + model + params (key-value list)
    val modelForm = JPanel(GridBagLayout())
    val editLLMProvidersInSettings = JPanel(BorderLayout())
    val modelGrid = GridBag().setDefaultInsets(JBUI.insets(4))

    var rebuildModelFormRef: () -> Unit = {}
    var scrollToBottomRef: () -> Unit = {}
    var rebuildParamsUIRef: () -> Unit = {}

    fun makeParamPair(@NlsSafe k: String? = null, @NlsSafe v: String? = null): Pair<JBTextField, JBTextField> {
        val kf = JBTextField(k.orEmpty())
        val vf = JBTextField(v.orEmpty())
        kf.columns = 20
        vf.columns = 20
        return kf to vf
    }

    val reportedEvalConfigLLMProviderBannerShown = MutableStateFlow(false)
    val reportedEvalConfigLLMProviderBannerImportKeysButtonShown = MutableStateFlow(false)

    fun buildModelFormContent() {
        modelForm.removeAll()
        editLLMProvidersInSettings.removeAll()

        if (viewModel.availableProviders.value.isEmpty()) {
            if (!reportedEvalConfigLLMProviderBannerShown.value) {
                EvaluationCollector.reportEvalConfigLLMProviderBannerShown(project)
                reportedEvalConfigLLMProviderBannerShown.value = true
            }
            val providers = viewModel.availableProviders.value
            val newKeys = ApiKeysInEnvVariablesService.getInstance(project).newKeys.value
            val newApiKeysNotInCurrentProviders =
                newKeys.filter { !providers.any { provider -> provider.providerType == it.key } }
            val text = if (newApiKeysNotInCurrentProviders.isNotEmpty()) {
                AIToolkitUIBundle.message("notification.eval.config.popup.api.keys.found.text")
            } else {
                AIToolkitUIBundle.message("notification.eval.config.popup.api.keys.notfound.text")
            }
            val link = Wrapper(
                InlineBanner(EditorNotificationPanel.Status.Info)
                    .setMessage(text)
                    .apply {
                        if (newApiKeysNotInCurrentProviders.isNotEmpty()) {
                            if (!reportedEvalConfigLLMProviderBannerImportKeysButtonShown.value) {
                                EvaluationCollector.reportEvalConfigLLMProviderBannerImportKeysButtonShown(project)
                                reportedEvalConfigLLMProviderBannerImportKeysButtonShown.value = true
                            }
                            addAction(AIToolkitUIBundle.message("notification.eval.config.popup.api.keys.found.action")) {
                                EvaluationCollector.reportEvalConfigLLMProviderBannerImportKeysButtonClicked(project)
                                val success = showSettingsAndEnvApiKeysDialog(project, newApiKeysNotInCurrentProviders)
                                if (success) {
                                    EvaluationCollector.reportEvalConfigLLMProviderImportKeysSuccess(project)
                                }
                                viewModel.save()
                                viewModel.reloadProviders()

                                SwingUtilities.invokeLater {
                                    cbProvider.removeAllItems()
                                    viewModel.availableProviders.value.forEach { cbProvider.addItem(it) }
                                    cbProvider.selectedItem = viewModel.selectedProvider.value
                                    cbProvider.isEnabled = viewModel.availableProviders.value.isNotEmpty()
                                    rebuildModelFormRef()
                                    scrollToBottomRef()
                                }
                            }
                        }
                    }.addAction(AIToolkitUIBundle.message("notification.eval.config.popup.api.keys.found.action2")) {
                        EvaluationCollector.reportEvalConfigLLMProviderBannerGoToSettingsButtonClicked(project)
                        ShowSettingsUtil.getInstance().showSettingsDialog(
                            project,
                            EvaluationBundle.message("eval.config.settings.name")
                        )
                        viewModel.save()
                        viewModel.reloadProviders()

                        SwingUtilities.invokeLater {
                            cbProvider.removeAllItems()
                            viewModel.availableProviders.value.forEach { cbProvider.addItem(it) }
                            cbProvider.selectedItem = viewModel.selectedProvider.value
                            cbProvider.isEnabled = viewModel.availableProviders.value.isNotEmpty()
                            rebuildModelFormRef()
                            scrollToBottomRef()
                        }
                    }.showCloseButton(false)
            ).apply {
                border = JBUI.Borders.empty(4)
                maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
            }
            modelForm.add(
                link,
                modelGrid.nextLine().coverLine().weightx(1.0).anchor(GridBagConstraints.WEST).fillCellHorizontally()
            )
        } else {
            modelForm.add(
                JLabel(EvaluationBundle.message("eval.config.model.provider.label")),
                modelGrid.nextLine().next().weightx(0.0).anchor(GridBagConstraints.WEST).insets(JBUI.insets(0, 4, 0, 8))
            )
            modelForm.add(cbProvider, modelGrid.next().weightx(1.0).fillCellHorizontally())

            modelForm.add(
                JLabel(EvaluationBundle.message("eval.config.model.model.label")),
                modelGrid.nextLine().next().weightx(0.0).anchor(GridBagConstraints.WEST).insets(JBUI.insets(0, 4, 0, 8))
            )
            modelForm.add(cbModel, modelGrid.next().weightx(1.0).fillCellHorizontally())

            val paramsRowsPanel = JPanel(GridBagLayout())
            val paramsRightPanel = JPanel(BorderLayout())

            fun buildParamRowPanel(idx: Int, kField: JBTextField, vField: JBTextField): JPanel {
                val rowPanel = JPanel(BorderLayout(0, 0))

                val fieldsPanel = JPanel(GridBagLayout())
                val fgb = GridBag().setDefaultInsets(JBUI.emptyInsets())

                val comboBoxInset = runCatching {
                    cbProvider.insets?.left ?: cbProvider.border?.getBorderInsets(cbProvider)?.left ?: 0
                }.getOrDefault(0)

                val textFieldInset = runCatching {
                    kField.insets?.left ?: kField.border?.getBorderInsets(kField)?.left ?: 0
                }.getOrDefault(0)

                val alignmentOffset = (comboBoxInset - textFieldInset).coerceAtLeast(0)

                fieldsPanel.add(
                    kField,
                    fgb.nextLine().next()
                        .insets(JBUI.insetsLeft(alignmentOffset))
                        .weightx(0.5)
                        .fillCellHorizontally()
                )
                fieldsPanel.add(
                    vField,
                    fgb.next()
                        .insets(JBUI.insets(0, 8 + alignmentOffset, 0, 0))
                        .weightx(0.5)
                        .fillCellHorizontally()
                )

                rowPanel.add(fieldsPanel, BorderLayout.CENTER)

                val btnRemove = createSquareIconButton(AllIcons.Actions.GC)
                btnRemove.toolTipText = EvaluationBundle.message("eval.config.param.remove.tooltip")
                btnRemove.addActionListener {
                    paramRows.removeAt(idx)
                    rebuildParamsUIRef()
                }

                val buttonWrapper = JPanel(BorderLayout()).apply {
                    border = JBUI.Borders.emptyLeft(8)
                    add(btnRemove, BorderLayout.CENTER)
                }
                rowPanel.add(buttonWrapper, BorderLayout.EAST)

                return rowPanel
            }

            val btnAddParam = JButton(EvaluationBundle.message("eval.config.params.add")).apply {
                toolTipText = EvaluationBundle.message("eval.config.param.add.tooltip")
                addActionListener {
                    paramRows.add(makeParamPair())
                    rebuildParamsUIRef()
                }
            }

            fun rebuildParamsUI() {
                paramsRowsPanel.removeAll()
                paramsRightPanel.removeAll()

                val grid = GridBag().setDefaultInsets(JBUI.insets(2))

                val comboBoxInset = runCatching {
                    cbProvider.insets?.left ?: cbProvider.border?.getBorderInsets(cbProvider)?.left ?: 0
                }.getOrDefault(0)

                val textFieldInset = if (paramRows.isNotEmpty()) {
                    runCatching {
                        val kField = paramRows[0].first
                        kField.insets?.left ?: kField.border?.getBorderInsets(kField)?.left ?: 0
                    }.getOrDefault(0)
                } else {
                    0
                }

                val alignmentOffset = (comboBoxInset - textFieldInset).coerceAtLeast(0)

                paramRows.forEachIndexed { idx, (kField, vField) ->
                    val rowPanel = buildParamRowPanel(idx, kField, vField)
                    paramsRowsPanel.add(
                        rowPanel,
                        grid.nextLine().next()
                            .weightx(1.0)
                            .fillCellHorizontally()
                    )
                }

                val addButtonWrapper = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                    border = JBUI.Borders.empty(2, alignmentOffset, 0, 0)
                    add(btnAddParam)
                }

                paramsRowsPanel.add(
                    addButtonWrapper,
                    grid.nextLine().next()
                        .weightx(1.0)
                        .fillCellHorizontally()
                )

                paramsRightPanel.add(paramsRowsPanel, BorderLayout.CENTER)

                paramsRowsPanel.revalidate()
                paramsRowsPanel.repaint()
                paramsRightPanel.revalidate()
                paramsRightPanel.repaint()
            }

            rebuildParamsUIRef = { rebuildParamsUI() }

            modelForm.add(
                JLabel(EvaluationBundle.message("eval.config.model.params.label")),
                modelGrid.nextLine().next()
                    .weightx(0.0)
                    .anchor(GridBagConstraints.NORTHWEST)  // Top-align with first row
                    .insets(JBUI.insets(0, 4, 0, 8))
            )
            modelForm.add(
                paramsRightPanel,
                modelGrid.next()
                    .weightx(1.0)
                    .fillCellHorizontally()
            )

            rebuildParamsUI()

            editLLMProvidersInSettings.apply {
                border = JBUI.Borders.emptyRight(JBUI.scale(16))
                add(
                    ActionLink(EvaluationBundle.message("eval.config.model.edit.providers")) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(
                            project,
                            EvaluationBundle.message("eval.config.settings.name")
                        )
                        viewModel.save()
                        viewModel.reloadProviders()

                        SwingUtilities.invokeLater {
                            cbProvider.removeAllItems()
                            viewModel.availableProviders.value.forEach { cbProvider.addItem(it) }
                            cbProvider.selectedItem = viewModel.selectedProvider.value
                            cbProvider.isEnabled = viewModel.availableProviders.value.isNotEmpty()
                            rebuildModelFormRef()
                            scrollToBottomRef()
                        }
                    }
                )

                revalidate()
                repaint()
            }
        }

        modelForm.revalidate()
        modelForm.repaint()
    }

    rebuildModelFormRef = { buildModelFormContent() }
    buildModelFormContent()

    viewModel.modelParams
        .onEach { params ->
            paramRows.clear()
            params.forEach { (k, v) ->
                paramRows.add(makeParamPair(k, v))
            }
            if (viewModel.availableProviders.value.isNotEmpty()) {
                rebuildParamsUIRef()
            }
        }
        .launchIn(scope)

    viewModel.availableProviders
        .onEach { list ->
            isUpdatingProvider = true
            val current = cbProvider.selectedItem
            cbProvider.removeAllItems()
            list.forEach { cbProvider.addItem(it) }
            cbProvider.isEnabled = list.isNotEmpty()
            if (current != null && list.contains(current)) {
                cbProvider.selectedItem = current
            }
            isUpdatingProvider = false
            rebuildModelFormRef()
        }
        .launchIn(scope)

    fun makeSection(
        @Nls title: String,
        content: JComponent,
        expanded: Boolean = true,
        rightComponent: JComponent? = null
    ): JPanel {
        val wrapper = JPanel(BorderLayout())
        var isExpanded = expanded
        val headerButton = JButton(title).apply {
            isContentAreaFilled = false
            isBorderPainted = false
            isFocusPainted = false
            horizontalAlignment = SwingConstants.LEFT
            iconTextGap = JBUI.scale(6)
            border = JBUI.Borders.empty(6, 4)
        }

        fun updateHeader() {
            headerButton.icon = if (isExpanded) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
            content.isVisible = isExpanded
        }
        headerButton.addActionListener {
            isExpanded = !isExpanded
            updateHeader()
            wrapper.revalidate()
            wrapper.repaint()
        }
        updateHeader()
        val headerPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(headerButton, BorderLayout.CENTER)
            if (rightComponent != null) {
                add(rightComponent, BorderLayout.EAST)
            }
        }
        wrapper.add(headerPanel, BorderLayout.NORTH)
        wrapper.add(content, BorderLayout.CENTER)
        return wrapper
    }

    val generalSection = makeSection(EvaluationBundle.message("eval.config.section.general"), generalForm, true)
    val llmProviderSection = makeSection(EvaluationBundle.message("eval.config.section.llm.provider"), modelForm, true, editLLMProvidersInSettings)
    val evaluatorsSection = makeSection(EvaluationBundle.message("eval.config.section.evaluators"), evaluatorsWrapper, true)

    val sectionRightMargin = JBUI.scale(20) // Accommodates scrollbar (~16px) plus extra spacing
    generalForm.border = JBUI.Borders.emptyRight(sectionRightMargin)
    evaluatorsPanel.border = JBUI.Borders.emptyRight(sectionRightMargin)
    modelForm.border = JBUI.Borders.emptyRight(sectionRightMargin)

    val sectionsPanel = JPanel(GridBagLayout())

    viewModel.evaluators
        .onEach { evaluators ->
            val hasLlmJudge = evaluators.any { it.type == "llm judge" }
            if (llmProviderSection.isVisible != hasLlmJudge) {
                llmProviderSection.isVisible = hasLlmJudge
                sectionsPanel.revalidate()
                sectionsPanel.repaint()
            }
        }
        .launchIn(scope)

    rebuildEvaluatorsUI()

    val sgb = GridBag().setDefaultInsets(JBUI.insets(4, 0, 8, 0))
    sectionsPanel.add(
        generalSection,
        sgb.nextLine().next().weightx(1.0).fillCellHorizontally().anchor(GridBagConstraints.NORTHWEST)
    )
    sectionsPanel.add(
        evaluatorsSection,
        sgb.nextLine().next().weightx(1.0).fillCellHorizontally().anchor(GridBagConstraints.NORTHWEST)
    )
    sectionsPanel.add(
        llmProviderSection,
        sgb.nextLine().next().weightx(1.0).fillCellHorizontally().anchor(GridBagConstraints.NORTHWEST)
    )
    sectionsPanel.add(JPanel(), sgb.nextLine().next().weightx(1.0).weighty(1.0).fillCellVertically())

    val headerPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 6)).apply {
        add(JLabel(EvaluationBundle.message("eval.config.row.run.config")))
        add(cbRunConfig)
    }

    cbRunConfig.addActionListener {
        if (!isUpdatingRunConfig) {
            viewModel.setRunConfigName(cbRunConfig.selectedItem as? String ?: "")
        }
    }

    val contentScroll = JBScrollPane(sectionsPanel).apply {
        border = null
        viewportBorder = null
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
    }

    scrollToBottomRef = {
        SwingUtilities.invokeLater {
            try {
                contentScroll.verticalScrollBar?.value = contentScroll.verticalScrollBar?.maximum ?: 0
            } catch (_: Throwable) { }
        }
    }

    val container = JPanel(BorderLayout()).apply {
        add(headerPanel, BorderLayout.NORTH)
        add(contentScroll, BorderLayout.CENTER)
    }

    // Validation
    var applyOk: (Boolean) -> Unit = {}

    fun updateValidation() {
        val allValid = viewModel.isValid.value
        applyOk(allValid)
    }

    viewModel.isValid
        .onEach { isValid ->
            applyOk(isValid)
        }
        .launchIn(scope)

    cbDataset.addActionListener {
        if (!isUpdatingDataset) {
            viewModel.setSelectedDataset(cbDataset.selectedItem as? String ?: "")
            updateValidation()
        }
    }

    cbProvider.addActionListener {
        if (!isUpdatingProvider) {
            val provider = cbProvider.selectedItem as? ProviderInstance
            viewModel.onProviderSelected(provider)
        }
    }

    tfName.document.addDocumentListener(object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) = viewModel.setName(tfName.text)
        override fun removeUpdate(e: DocumentEvent?) = viewModel.setName(tfName.text)
        override fun changedUpdate(e: DocumentEvent?) = viewModel.setName(tfName.text)
    })

    updateValidation()

    val formHeight = sectionsPanel.preferredSize.height
    val headerHeight = headerPanel.preferredSize.height
    val computedHeight = min(formHeight + headerHeight + 2, targetHeight)
    container.preferredSize = Dimension(targetWidth, computedHeight)

    fun doSave() {
        viewModel.setName(tfName.text)
        viewModel.setDescription(taDesc.text)
        viewModel.setSelectedDataset(cbDataset.selectedItem as? String ?: "")
        viewModel.setRunConfigName(cbRunConfig.selectedItem as? String ?: "")
        viewModel.setSelectedModel(cbModel.selectedItem as? LlmModel)

        val params = paramRows.mapNotNull { (kField, vField) ->
            val k = kField.text.trim()
            val v = vField.text
            if (k.isNotEmpty()) k to v else null
        }
        viewModel.setModelParams(params)

        val savedName = viewModel.save()
        onSaved?.invoke(savedName)
    }

    val dialog = object : DialogWrapper(
        project,
        true,
        // Make dialog modeless while onboarding is active so onboarding popup stays clickable
        if (OnboardingRuntimeFlags.onboardingActive) IdeModalityType.MODELESS else IdeModalityType.IDE
    ) {
        init {
            title = if (existing == null) EvaluationBundle.message("eval.dialog.title.create") else EvaluationBundle.message("eval.dialog.title.edit")
            isResizable = true
            isModal = false
            init()
        }

        override fun createCenterPanel(): JComponent = container

        override fun doOKAction() {
            EvaluationCollector.reportEvalConfigCreated(project)
            try {
                if (OnboardingRuntimeFlags.onboardingActive) {
                    AnchorBus.sink?.setFlag(
                        OnboardingAnchorKeys.CREATE_CONFIG_CONFIRMED,
                        true
                    )
                }
            } catch (_: Throwable) { }
            doSave()
            scope.cancel()
            super.doOKAction()
        }

        override fun doCancelAction() {
            try {
                if (OnboardingRuntimeFlags.onboardingActive) {
                    AnchorBus.sink?.setFlag(
                        OnboardingAnchorKeys.CREATE_CONFIG_CONFIRMED,
                        true
                    )
                }
            } catch (_: Throwable) { }
            scope.cancel()
            super.doCancelAction()
        }
    }

    closeDialogRef = { dialog.close(DialogWrapper.OK_EXIT_CODE) }
    Disposer.register(dialog.disposable, uiDisposable)

    applyOk = { ok ->
        try { dialog.isOKActionEnabled = ok } catch (_: Throwable) { }
    }
    updateValidation()

    // Ensure all inputs scroll to the beginning and carets are at start when opened
    SwingUtilities.invokeLater {
        try {
            tfName.caretPosition = 0
            taDesc.caretPosition = 0
            paramRows.forEach { (kField, vField) ->
                kField.caretPosition = 0
                vField.caretPosition = 0
            }
            descScroll.verticalScrollBar?.value = 0
            descScroll.horizontalScrollBar?.value = 0
            contentScroll.verticalScrollBar?.value = 0
            contentScroll.horizontalScrollBar?.value = 0
        } catch (_: Throwable) { }
    }

    SwingUtilities.invokeLater { registerOnboardingAnchorConfigDialog(dialog, container) }

    dialog.show()
}

private fun registerOnboardingAnchorConfigDialog(dialog: DialogWrapper, container: JPanel) {
    try {
        val w = dialog.peer.window ?: SwingUtilities.getWindowAncestor(container)
        if (w != null && w.isShowing) {
            val wl = w.locationOnScreen
            val rect = androidx.compose.ui.geometry.Rect(
                wl.x.toFloat(), wl.y.toFloat(),
                (wl.x + w.width).toFloat(), (wl.y + w.height).toFloat()
            )
            AnchorBus.sink?.set(OnboardingAnchorKeys.CREATE_CONFIG_DIALOG_RUN_CONFIGURATION, rect)
            AnchorBus.sink?.setFlag(OnboardingAnchorKeys.CREATE_CONFIG_DIALOG_OPEN, true)
        }
    } catch (_: Throwable) { }
}

@OptIn(FlowPreview::class)
private fun openPromptInEditor(
    project: Project,
    repository: EvalConfigsRepository,
    configName: String,
    evaluatorName: String,
    initialContent: String
) {
    val fileType = FileTypeManager.getInstance().getFileTypeByExtension("md")
    val safeConfigName = configName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    val safeEvaluatorName = evaluatorName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    val fileName = "${safeConfigName}_${safeEvaluatorName}_prompt.md"

    val virtualFile = LightVirtualFile(fileName, fileType, initialContent)
    FileEditorManager.getInstance(project).openFile(virtualFile, true)

    val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return

    val connection = project.messageBus.connect()
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
        override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
            if (file == virtualFile) {
                connection.disconnect()
            }
        }
    })

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    Disposer.register(connection) {
        scope.cancel()
    }

    val updateFlow = MutableStateFlow(initialContent)

    var cachedInfo = repository.listConfigs().firstOrNull { it.name == configName }
    var cachedConfig = cachedInfo?.let { repository.loadConfig(it) }

    updateFlow
        .debounce(500)
        .onEach { newPrompt ->
            try {
                if (cachedConfig == null) {
                    cachedInfo = repository.listConfigs().firstOrNull { it.name == configName }
                    cachedConfig = cachedInfo?.let { repository.loadConfig(it) }
                }

                val path = repository.getConfigPathByName(configName)
                if (path == null || !java.nio.file.Files.exists(path)) {
                    return@onEach
                }

                val currentConfig = cachedConfig ?: return@onEach
                val evaluators = currentConfig.evaluators?.toMutableList()
                if (evaluators != null) {
                    val index = evaluators.indexOfFirst { it.name == evaluatorName }
                    if (index != -1) {
                        val currentEval = evaluators[index]
                        if (currentEval.prompt != newPrompt) {
                            evaluators[index] = currentEval.copy(prompt = newPrompt)
                            val newConfig = currentConfig.copy(evaluators = evaluators)

                            cachedConfig = newConfig
                            cachedInfo = repository.createOrUpdateConfig(configName, newConfig)
                        }
                    }
                }
            } catch (_: Exception) { }
        }
        .launchIn(scope)

    document.addDocumentListener(object : com.intellij.openapi.editor.event.DocumentListener {
        override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
            updateFlow.value = event.document.text
        }
    }, connection)
}