package com.jetbrains.bigdatatools.flink.submit

import com.intellij.bigdatatools.coreUi.settings.getValidationInfo
import com.intellij.bigdatatools.coreUi.settings.withNonEmptyValidator
import com.intellij.bigdatatools.coreUi.ui.row
import com.intellij.bigdatatools.coreUi.ui.shortRow
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.IntegerField
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.bigdatatools.flink.util.FlinkMessagesBundle

// More or less actual parameters list could be found here
// https://nightlies.apache.org/flink/flink-docs-release-1.10/ops/cli.html
class RunJarStepDialog(project: Project,
                       @NlsSafe entryClassName: String,
                       @NlsContexts.DialogTitle title: String) : DialogWrapper(project) {

  private val entryClassNameField = JBTextField(entryClassName).apply {
    withNonEmptyValidator(disposable)
  }

  private val argumentsField = JBTextField(15)

  private val parallelismField = IntegerField().apply {
    columns = 6
    isCanBeEmpty = true
    toolTipText = FlinkMessagesBundle.message("dialog.jar.run.parallelism.descr")
  }

  private val savepointPathField = JBTextField(15).apply {
    toolTipText = FlinkMessagesBundle.message("dialog.jar.run.savepoint.path.descr")
  }

  private val allowNonRestoredCheckbox = JBCheckBox(FlinkMessagesBundle.message("dialog.jar.run.allow.checkbox.text")).apply {
    toolTipText = FlinkMessagesBundle.message("dialog.jar.run.allow.checkbox.descr")
  }

  init {
    this.title = title
    super.init()
  }

  override fun createCenterPanel() = panel {
    row(allowNonRestoredCheckbox)
    row(FlinkMessagesBundle.message("dialog.jar.run.entryClass.label"), entryClassNameField)
    row(FlinkMessagesBundle.message("dialog.jar.run.arguments.label"), argumentsField)
    shortRow(FlinkMessagesBundle.message("dialog.jar.run.parallelism.label"), parallelismField)
    row(FlinkMessagesBundle.message("dialog.jar.run.savepoint.path.label"), savepointPathField)
  }

  fun getResult(): Map<String, Any?> {
    val data = mutableMapOf<String, Any?>()
    data["allowNonRestoredState"] = allowNonRestoredCheckbox.isSelected
    data["entryClass"] = entryClassNameField.text
    data["parallelism"] = parallelismField.text.toIntOrNull() ?: 1
    data["programArgs"] = argumentsField.text
    data["savepointPath"] = savepointPathField.text
    return data
  }

  override fun doValidate() = entryClassNameField.getValidationInfo()
}