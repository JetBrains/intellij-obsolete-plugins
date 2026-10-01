package com.intellij.aidebugger.common.views.components

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.common.models.HierarchicalTraceEventsState
import com.intellij.aidebugger.common.onboarding.AnchorBus
import com.intellij.aidebugger.common.onboarding.OnboardingAnchorKeys
import com.intellij.aidebugger.common.onboarding.OnboardingRuntimeFlags
import com.intellij.aidebugger.common.utility.JsonPathUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.EditorTextField
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.border.Border
import kotlin.coroutines.resume
import javax.swing.event.DocumentEvent as SwingDocumentEvent
import javax.swing.event.DocumentListener as SwingDocumentListener

class AddTraceDialog(
    project: Project,
    datasetName: String,
    @NlsSafe defaultInputPath: String,
    defaultInputValue: String,
    @NlsSafe defaultExpectedPath: String,
    rawJson: String,
    val hierarchicalState: HierarchicalTraceEventsState?
) : DialogWrapper(
    project,
    true,
    // If onboarding is active, keep the dialog modeless so it doesn't block onboarding popup clicks
    if (OnboardingRuntimeFlags.onboardingActive) IdeModalityType.MODELESS else IdeModalityType.IDE
) {
    private val projectRef: Project = project
    private var resultContinuation: CancellableContinuation<Result>? = null

    data class Result(
        val confirmed: Boolean,
        val inputPath: String,
        val inputValue: String,
        val expectedPath: String,
        val expectedValue: String,
        val rawJson: String,
    )

    private val inputPathField = JTextField(defaultInputPath)
    private val expectedPathField = JTextField(defaultExpectedPath)

    private var isInputPathValid: Boolean = false
    private var isExpectedPathValid: Boolean = false

    private val prettyInput: String = try {
        val parsed = JsonParser.parseString(defaultInputValue)
        GsonBuilder().setPrettyPrinting().create().toJson(parsed)
    } catch (_: Throwable) { defaultInputValue }

    private val prettyRaw: String = try {
        val parsed = JsonParser.parseString(rawJson)
        GsonBuilder().setPrettyPrinting().create().toJson(parsed)
    } catch (_: Throwable) { rawJson }

    private fun looksLikeJsonStructure(text: String): Boolean {
        return try {
            val el = JsonParser.parseString(text)
            el.isJsonObject || el.isJsonArray
        } catch (_: Throwable) {
            false
        }
    }

    private fun parseRawJson(): JsonElement? = try { JsonParser.parseString(rawEditor.text) } catch (_: Throwable) { null }

    private fun toDisplayString(el: JsonElement?): String {
        if (el == null) return ""
        return if (el.isJsonObject || el.isJsonArray) GsonBuilder().setPrettyPrinting().create().toJson(el)
        else JsonPathUtils.jsonStringLike(el) ?: ""
    }

    private val defaultInputPathBorder: Border = inputPathField.border
    private val defaultExpectedPathBorder: Border = expectedPathField.border
    private val errorBorder: Border = BorderFactory.createLineBorder(Color(0xD0, 0x3C, 0x3C), 1)

    private fun setPathFieldValidity(field: JTextField, valid: Boolean) {
        field.border = if (valid) {
            if (field === inputPathField) defaultInputPathBorder else defaultExpectedPathBorder
        } else errorBorder
    }

    private fun resolvePath(root: JsonElement?, path: String): Pair<JsonElement?, String?> {
        if (root == null) return null to null
        val tokens = try {
            JsonPathUtils.parseJsonPath(path)
        } catch (e: Throwable) {
            return null to (e.message ?: "Invalid path syntax")
        }
        if (tokens.isEmpty()) return root to null
        val result = try {
            JsonPathUtils.getValueByPath(root, tokens)
        } catch (e: Throwable) {
            return null to (e.message ?: "Error resolving path")
        }
        return if (result != null) result to null else null to "Path not found in JSON"
    }

    private fun EditorTextField.configureScrolling() {
        setOneLineMode(false)
        addSettingsProvider { editor ->
            try {
                editor?.let { ee ->
                    ee.settings.isUseSoftWraps = true
                    ee.settings.isWhitespacesShown = false
                    ee.setVerticalScrollbarVisible(true)
                    ee.setHorizontalScrollbarVisible(true)
                }
            } catch (_: Throwable) { }
            SwingUtilities.invokeLater {
                try { scrollEditorToTop(this@configureScrolling) } catch (_: Throwable) { }
            }
        }
    }

    private fun EditorTextField.configureDynamicJsonHighlighting() {
        val jsonType = FileTypeManager.getInstance().getFileTypeByExtension("json")
        val plainType = PlainTextFileType.INSTANCE
        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                try {
                    val txt = text
                    val isJson = looksLikeJsonStructure(txt)
                    val targetType = if (isJson) jsonType else plainType
                    if (this@configureDynamicJsonHighlighting.fileType != targetType) {
                        try {
                            this@configureDynamicJsonHighlighting.setFileType(targetType)
                        } catch (_: Throwable) { }
                    }
                } catch (_: Throwable) { }
            }
        })
    }

    private val inputEditor: EditorTextField = run {
        val jsonType = FileTypeManager.getInstance().getFileTypeByExtension("json")
        val plainType = PlainTextFileType.INSTANCE
        val initialType = if (looksLikeJsonStructure(prettyInput)) jsonType else plainType
        EditorTextField(prettyInput, projectRef, initialType).apply {
            configureScrolling()
            configureDynamicJsonHighlighting()
            isEnabled = false
        }
    }

    private val rawEditor: EditorTextField = run {
        val jsonType = FileTypeManager.getInstance().getFileTypeByExtension("json")
        EditorTextField(prettyRaw, projectRef, jsonType).apply {
            configureScrolling()
        }
    }

    private val expectedEditor: EditorTextField = run {
        val plainType = PlainTextFileType.INSTANCE
        EditorTextField("", projectRef, plainType).apply {
            configureScrolling()
            configureDynamicJsonHighlighting()
        }
    }

    private fun scrollEditorToTop(field: EditorTextField) {
        try {
            val ed = field.editor
            if (ed != null) {
                ed.caretModel.moveToOffset(0)
                try {
                    ed.scrollingModel.scrollVertically(0)
                    ed.scrollingModel.scrollHorizontally(0)
                } catch (_: Throwable) {
                    ed.scrollingModel.scrollToCaret(ScrollType.CENTER_UP)
                }
            }
        } catch (_: Throwable) { }
    }

    private fun updateOkButtonState() {
        setOKActionEnabled(isInputPathValid && isExpectedPathValid)
    }

    private fun handleInputPathChanged(): Boolean {
        val root = parseRawJson()
        val path = inputPathField.text.orEmpty().trim()
        val (el, error) = resolvePath(root, path)
        val valid = el != null
        isInputPathValid = valid
        setPathFieldValidity(inputPathField, valid)
        inputPathField.toolTipText = if (valid) {
            AiDebuggerBundle.message("add.trace.input.path.tooltip")
        } else {
            error
        }

        if (valid) {
            val newText = toDisplayString(el)
            if (newText != inputEditor.text) {
                inputEditor.text = newText
                scrollEditorToTop(inputEditor)
                SwingUtilities.invokeLater {
                    try { scrollEditorToTop(inputEditor) } catch (_: Throwable) { }
                }
            }
        }
        updateOkButtonState()
        return valid
    }

    private fun handleExpectedPathChanged(): Boolean {
        val root = parseRawJson()
        val path = expectedPathField.text.orEmpty().trim()
        val (el, error) = resolvePath(root, path)
        val valid = el != null
        isExpectedPathValid = valid
        setPathFieldValidity(expectedPathField, valid)
        expectedPathField.toolTipText = if (valid) {
            AiDebuggerBundle.message("add.trace.expected.path.tooltip")
        } else {
            error
        }

        if (valid) {
            val newText = toDisplayString(el)
            if (newText != expectedEditor.text) {
                expectedEditor.text = newText
                scrollEditorToTop(expectedEditor)
                SwingUtilities.invokeLater {
                    try { scrollEditorToTop(expectedEditor) } catch (_: Throwable) { }
                }
            }
        }
        updateOkButtonState()
        return valid
    }

    override fun doCancelAction() {
        try {
            if (OnboardingRuntimeFlags.onboardingActive) {
                AnchorBus.sink?.setFlag(OnboardingAnchorKeys.ADD_TO_DATASET_DIALOG_CONFIRMED, true)
            }
        } catch (_: Throwable) { }
        super.doCancelAction()
    }

    override fun doOKAction() {
        try {
            if (OnboardingRuntimeFlags.onboardingActive) {
                AnchorBus.sink?.setFlag(OnboardingAnchorKeys.ADD_TO_DATASET_DIALOG_CONFIRMED, true)
            }
        } catch (_: Throwable) { }
        super.doOKAction()
    }

    init {
        title = AiDebuggerBundle.message("add.trace.dialog.title", datasetName)
        setOKButtonText(AiDebuggerBundle.message("add.trace.ok.button"))
        init()

        inputPathField.document.addDocumentListener(object : SwingDocumentListener {
            override fun insertUpdate(e: SwingDocumentEvent) { handleInputPathChanged() }
            override fun removeUpdate(e: SwingDocumentEvent) { handleInputPathChanged() }
            override fun changedUpdate(e: SwingDocumentEvent) { handleInputPathChanged() }
        })
        expectedPathField.document.addDocumentListener(object : SwingDocumentListener {
            override fun insertUpdate(e: SwingDocumentEvent) { handleExpectedPathChanged() }
            override fun removeUpdate(e: SwingDocumentEvent) { handleExpectedPathChanged() }
            override fun changedUpdate(e: SwingDocumentEvent) { handleExpectedPathChanged() }
        })
        rawEditor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                handleInputPathChanged()
                handleExpectedPathChanged()
            }
        })

        // TODO hierarchicalState unitialized? why it is not reported from here sometimes?
        val framework = hierarchicalState?.rootEvents?.firstOrNull()?.framework

        val defaultInputPathCorrectness = handleInputPathChanged()
        AiDebuggerCollector.reportDefaultInputJsonPathCorrectness(project, framework, defaultInputPathCorrectness)
        val defaultExpectedPathCorrectness = handleExpectedPathChanged()
        AiDebuggerCollector.reportDefaultExpectedJsonPathCorrectness(project, framework, defaultExpectedPathCorrectness)

        updateOkButtonState()

        SwingUtilities.invokeLater { registerOnboardingAnchorDatasetDialog() }
    }

    private fun registerOnboardingAnchorDatasetDialog() {
        try {
            val w = this.peer.window ?: SwingUtilities.getWindowAncestor(this.window)
            if (w != null && w.isShowing) {
                val wl = w.locationOnScreen
                val rect = androidx.compose.ui.geometry.Rect(
                    wl.x.toFloat(), wl.y.toFloat(),
                    (wl.x + w.width).toFloat(), (wl.y + w.height).toFloat()
                )
                AnchorBus.sink?.set(OnboardingAnchorKeys.ADD_TO_DATASET_DIALOG, rect)
                AnchorBus.sink?.setFlag(OnboardingAnchorKeys.ADD_TO_DATASET_DIALOG_OPEN, true)
            }
        } catch (_: Throwable) { }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(8, 8))
        panel.preferredSize = Dimension(900, 600)

        val content = JPanel(GridLayout(1, 2, 8, 8))

        val left = JPanel()
        left.layout = GridBagLayout()
        val lc = GridBagConstraints().apply {
            gridx = 0
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            insets = Insets(4, 4, 4, 4)
        }

        lc.gridy = 0
        left.add(JLabel(AiDebuggerBundle.message("add.trace.input.path.label")), lc)
        lc.gridy = 1
        inputPathField.toolTipText = AiDebuggerBundle.message("add.trace.input.path.tooltip")
        left.add(inputPathField, lc)

        lc.gridy = 2
        left.add(JLabel(AiDebuggerBundle.message("add.trace.input.label")), lc)
        lc.gridy = 3
        lc.fill = GridBagConstraints.BOTH
        lc.weighty = 1.0
        left.add(inputEditor, lc)

        val right = JPanel()
        right.layout = GridBagLayout()
        val rc = GridBagConstraints().apply {
            gridx = 0
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            insets = Insets(4, 4, 4, 4)
        }

        rc.gridy = 0
        right.add(JLabel(AiDebuggerBundle.message("add.trace.expected.path.label")), rc)
        rc.gridy = 1
        expectedPathField.toolTipText = AiDebuggerBundle.message("add.trace.expected.path.tooltip")
        right.add(expectedPathField, rc)

        rc.gridy = 2
        right.add(JLabel(AiDebuggerBundle.message("add.trace.expected.label")), rc)
        rc.gridy = 3
        rc.fill = GridBagConstraints.BOTH
        rc.weighty = 1.0
        right.add(expectedEditor, rc)

        content.add(left)
        content.add(right)
        panel.add(content, BorderLayout.CENTER)
        return panel
    }

    private fun createResult(): Result = Result(
        confirmed = isOK,
        inputPath = inputPathField.text.orEmpty(),
        inputValue = inputEditor.text.orEmpty(),
        expectedPath = expectedPathField.text.orEmpty(),
        expectedValue = expectedEditor.text.orEmpty(),
        rawJson = rawEditor.text.orEmpty(),
    )

    fun showForResult(): Result {
        show()
        return createResult()
    }

    suspend fun showAndAwait(): Result = suspendCancellableCoroutine { cont ->
        val framework = hierarchicalState?.rootEvents?.firstOrNull()?.framework
        AiDebuggerCollector.reportAddTracePopupShown(projectRef, framework)
        if (ApplicationManager.getApplication().isDispatchThread) {
            doShowAndAwait(cont)
        } else {
            ApplicationManager.getApplication().invokeLater {
                doShowAndAwait(cont)
            }
        }
    }

    private fun doShowAndAwait(cont: CancellableContinuation<Result>) {
        if (!isModal) {
            resultContinuation = cont
            show()
        } else {
            show()
            cont.resume(createResult())
        }
    }

    override fun dispose() {
        super.dispose()
        if (resultContinuation?.isActive == true) {
            resultContinuation?.resume(createResult())
            resultContinuation = null
        }
    }
}

suspend fun showAddTracePopup(
    project: Project,
    datasetName: String,
    @NlsSafe defaultInputPath: String,
    defaultInputValue: String,
    @NlsSafe defaultExpectedPath: String,
    rawJson: String,
    hierarchicalState: HierarchicalTraceEventsState?,
): AddTraceDialog.Result {
    return withContext(Dispatchers.Main) {
        val dlg = AddTraceDialog(
            project = project,
            datasetName = datasetName,
            defaultInputPath = defaultInputPath,
            defaultInputValue = defaultInputValue,
            defaultExpectedPath = defaultExpectedPath,
            rawJson = rawJson,
            hierarchicalState = hierarchicalState,
        )
        dlg.showAndAwait()
    }
}