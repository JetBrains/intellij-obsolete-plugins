package com.intellij.bigdatatools.zeppelin.ztools.settings

import com.intellij.CommonBundle
import com.intellij.bigdatatools.coreUi.ui.block
import com.intellij.bigdatatools.coreUi.fields.CustomListCellRenderer
import com.intellij.bigdatatools.coreUi.settings.revalidateComponentRecursive
import com.intellij.bigdatatools.coreUi.ui.wideCell
import com.intellij.bigdatatools.coreUi.settings.withPositiveIntValidator
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.CheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.bigdatatools.coreUi.ui.row
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.text.JTextComponent

object ZtoolsDialogBuilder {
  fun showAndGet(project: Project, initZtoolsConfig: ZtoolsConfig): ZtoolsConfig? {

    val builder = DialogBuilder(project)

    val enableVariableCollect = CheckBox(ZepMessagesBundle.message("ztools.settings.values.enable.variables"),
                                         initZtoolsConfig.variablesSettings.isEnabled).apply {
      toolTipText = ZepMessagesBundle.message("ztools.settings.values.enable.variables.hint")
    }

    val variableViewOnDemandOnly = CheckBox(ZepMessagesBundle.message("ztools.settings.values.variables.on.demand"),
                                            initZtoolsConfig.variablesSettings.isOnDemandOnly).apply {
      toolTipText = ZepMessagesBundle.message("ztools.settings.values.variables.on.demand.hint")
    }

    val variablesSameNoteOnly = CheckBox(ZepMessagesBundle.message("ztools.settings.values.variables.same.note.only"),
                                         initZtoolsConfig.variablesSettings.sameNoteOnly).apply {
      toolTipText = ZepMessagesBundle.message("ztools.settings.values.variables.same.note.only.hint")
    }

    fun createFieldWithPositiveIntValidator(initialValue: Int): JTextComponent {
      return JBTextField(initialValue.toString(), 10).withPositiveIntValidator(builder)
    }

    val variablesTimeout = createFieldWithPositiveIntValidator(initZtoolsConfig.variablesSettings.timeout).apply {
      toolTipText = ZepMessagesBundle.message("ztools.settings.values.variables.timeout.hint")
    }

    val variablesVarTimeout = createFieldWithPositiveIntValidator(initZtoolsConfig.variablesSettings.variableTimeout).apply {
      toolTipText = ZepMessagesBundle.message("ztools.settings.values.variables.value.timeout.hint")
    }

    val collectionSizeLimit = createFieldWithPositiveIntValidator(initZtoolsConfig.variablesSettings.collectionSizeLimit).apply {
      toolTipText = ZepMessagesBundle.message("ztools.settings.values.variables.collection.size.limit.hint")
    }

    val stringSizeLimit = createFieldWithPositiveIntValidator(initZtoolsConfig.variablesSettings.stringSizeLimit).apply {
      toolTipText = ZepMessagesBundle.message("ztools.settings.values.variables.string.size.limit.hint")
    }

    val interpreterResCountLimit = createFieldWithPositiveIntValidator(initZtoolsConfig.variablesSettings.interpreterResCountLimit).apply {
      toolTipText = ZepMessagesBundle.message("ztools.settings.values.variables.res.limit.hint")
    }

    val depth = createFieldWithPositiveIntValidator(initZtoolsConfig.variablesSettings.depth).apply {
      toolTipText = ZepMessagesBundle.message("ztools.settings.values.variables.depth.hint")
    }

    val enableProfiling = CheckBox(ZepMessagesBundle.message("ztools.settings.values.variables.enable.profiling"),
                                   initZtoolsConfig.profiling).apply {
      toolTipText = ZepMessagesBundle.message("ztools.settings.enable.profiling.hint")
    }

    val enableSqlCollect = CheckBox(ZepMessagesBundle.message("ztools.settings.values.sql.enable"),
                                    initZtoolsConfig.sqlSettings.isEnabled).apply {
      toolTipText = ZepMessagesBundle.message("ztools.settings.values.sql.enable.hint")
    }

    val sqlRefreshStrategy = ComboBox(ZtoolsSqlCollectStrategy.entries.toTypedArray()).apply {
      prototypeDisplayValue = ZtoolsSqlCollectStrategy.ONLY_ON_REFRESH
      selectedItem = initZtoolsConfig.sqlSettings.collectionStrategy
      isSwingPopup = false
      renderer = CustomListCellRenderer<ZtoolsSqlCollectStrategy> {
        it.desc
      }
    }

    val sqlFilterTable = ZtoolsSqlFilterTableComponent(project, initZtoolsConfig.sqlSettings.tableFilters)

    val collectOnlyTempTables = CheckBox(ZepMessagesBundle.message("ztools.settings.values.sql.collect.only.temp.tables"),
                                         initZtoolsConfig.sqlSettings.collectOnlyTempTables).apply {
      toolTipText = ZepMessagesBundle.message("ztools.settings.values.sql.collect.only.temp.tables.hint")
    }

    val sqlTimeout = createFieldWithPositiveIntValidator(initZtoolsConfig.sqlSettings.timeout.toInt()).apply {
      toolTipText = ZepMessagesBundle.message("ztools.settings.values.sql.timeout.hint")
    }

    val centralPanel = panel {
      collapsibleGroup(ZepMessagesBundle.message("ztools.settings.common.title")) {
        row(enableVariableCollect)
        row(enableSqlCollect)
        row(enableProfiling)
      }.expanded = true

      collapsibleGroup(ZepMessagesBundle.message("ztools.settings.variables.title")) {
        row(variableViewOnDemandOnly)
        row(variablesSameNoteOnly)
        row(ZepMessagesBundle.message("ztools.settings.values.variables.timeout")) { cell(variablesTimeout) }
        row(ZepMessagesBundle.message("ztools.settings.values.variables.value.timeout")) { cell(variablesVarTimeout) }
        row(ZepMessagesBundle.message("ztools.settings.values.variables.collection.size.limit")) { cell(collectionSizeLimit) }
        row(ZepMessagesBundle.message("ztools.settings.values.variables.string.size.limit")) { cell(stringSizeLimit) }
        row(ZepMessagesBundle.message("ztools.settings.values.variables.res.limit")) { cell(interpreterResCountLimit) }
        row(ZepMessagesBundle.message("ztools.settings.values.variables.depth")) { cell(depth) }
      }.expanded = false

      collapsibleGroup(ZepMessagesBundle.message("ztools.settings.sql.title")) {
        row(ZepMessagesBundle.message("ztools.settings.values.sql.timeout")) { cell(sqlTimeout) }
        row(ZepMessagesBundle.message("ztools.settings.values.sql.collection.strategy")) { wideCell(sqlRefreshStrategy) }
        block(sqlFilterTable.getComponent())
        row(collectOnlyTempTables)
      }.expanded = false
    }

    val okAction = object : AbstractAction(CommonBundle.getOkButtonText()) {
      init {
        putValue(DialogWrapper.DEFAULT_ACTION, true)
      }

      override fun actionPerformed(e: ActionEvent?) {
        if (centralPanel.revalidateComponentRecursive()) {
          builder.dialogWrapper.close(DialogWrapper.OK_EXIT_CODE)
        }
      }
    }

    builder.apply {
      // addOkAction()
      addAction(okAction)
      addCancelAction()
      title(ZepMessagesBundle.message("ztools.settings.dialog.title"))
      centerPanel(centralPanel)
      setHelpId("big.data.tools.notebooks.running.stateviewer")
    }

    if (!builder.showAndGet())
      return null

    val variablesSettings = ZtoolsVariablesSettings(isEnabled = enableVariableCollect.isSelected,
                                                    timeout = variablesTimeout.text.toInt(),
                                                    isOnDemandOnly = variableViewOnDemandOnly.isSelected,
                                                    sameNoteOnly = variablesSameNoteOnly.isSelected,
                                                    interpreterResCountLimit = interpreterResCountLimit.text.toInt(),
                                                    variableTimeout = variablesVarTimeout.text.toInt(),
                                                    stringSizeLimit = stringSizeLimit.text.toInt(),
                                                    collectionSizeLimit = collectionSizeLimit.text.toInt(),
                                                    depth = depth.text.toInt())

    val sqlSettings = ZtoolsSqlSettings(isEnabled = enableSqlCollect.isSelected,
                                        collectionStrategy = sqlRefreshStrategy.item,
                                        tableFilters = sqlFilterTable.getResult(),
                                        timeout = sqlTimeout.text.toLong(),
                                        collectOnlyTempTables = collectOnlyTempTables.isSelected)

    return ZtoolsConfig(variablesSettings = variablesSettings, sqlSettings = sqlSettings, profiling = enableProfiling.isSelected)
  }
}