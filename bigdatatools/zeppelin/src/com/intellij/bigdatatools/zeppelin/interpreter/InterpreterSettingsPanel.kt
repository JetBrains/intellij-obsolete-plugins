package com.intellij.bigdatatools.zeppelin.interpreter

import com.intellij.bigdatatools.coreUi.fields.CustomListCellRenderer
import com.intellij.bigdatatools.coreUi.settings.defaultui.UiUtil
import com.intellij.bigdatatools.coreUi.ui.MigPanel
import com.intellij.bigdatatools.zeppelin.components.service.ZeppelinInterpreterSettingsManager
import com.intellij.bigdatatools.zeppelin.models.interpreter.InstantiationType
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterInfo
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterOption
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterProperty
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterPropertyType
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterStatus
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBTextField
import com.intellij.ui.scale.JBUIScale
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.WrapLayout
import com.intellij.util.ui.table.ComboBoxTableCellEditor
import com.jetbrains.bigdatatools.common.table.ColumnWidthFittingStrategy
import com.jetbrains.bigdatatools.common.table.MaterialTableUtils
import com.jetbrains.bigdatatools.common.ui.MigBlock
import com.jetbrains.bigdatatools.common.ui.TableUiUtils
import net.miginfocom.layout.CC
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.util.function.Supplier
import java.util.regex.Pattern
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

@Suppress("HardCodedStringLiteral")
class InterpreterSettingsPanel(private val mode: Mode,
                               private val manager: ZeppelinInterpreterSettingsManager,
                               private val showPropertiesDetails: Boolean) : JPanel(BorderLayout()), Disposable {

  companion object {
    private const val SPLITTER_PROPORTION_KEY = "com.intellij.bigdatatools.zeppelin.interpreter.settings.splitter"
    private val INTERPRETER_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9-_]+\$")

    private val suitablePerNoteTypes = mapOf(
      Pair(InstantiationType.SHARED, listOf(InstantiationType.SHARED, InstantiationType.SCOPED, InstantiationType.ISOLATED)),
      Pair(InstantiationType.SCOPED, listOf(InstantiationType.NONE, InstantiationType.SCOPED, InstantiationType.ISOLATED)),
      Pair(InstantiationType.ISOLATED, listOf(InstantiationType.NONE, InstantiationType.SCOPED, InstantiationType.ISOLATED)))

    private val suitablePerUserTypes = mapOf(
      Pair(InstantiationType.SHARED, listOf(InstantiationType.SHARED)),
      Pair(InstantiationType.SCOPED,
           listOf(InstantiationType.NONE, InstantiationType.SHARED, InstantiationType.SCOPED, InstantiationType.ISOLATED)),
      Pair(InstantiationType.ISOLATED, listOf(InstantiationType.NONE, InstantiationType.SCOPED, InstantiationType.ISOLATED)),
      Pair(InstantiationType.NONE, listOf(InstantiationType.SCOPED, InstantiationType.ISOLATED)))
  }

  enum class Mode {
    CREATION,
    EDIT
  }

  private var currentSettings: InterpreterSettings? = null

  private val interpreterId = JBTextField(15)
  private val interpreterName = JBTextField(15)
  private val interpreterGroup = ComboBox<InterpreterTemplate?>().apply {
    isSwingPopup = false // To enable search in combo drop down.
    renderer = CustomListCellRenderer<InterpreterTemplate> { it.group }
  }
  private val interpreterErrorReason = JBTextField(15)
  private val interpreterStatus = StatusLabel()
  private val interpreterGroups = JPanel(WrapLayout(FlowLayout.LEADING))

  private var interpreterGroupBlock: MigBlock? = null

  // Options
  private val perNote = ComboBox(InstantiationType.values
                                   .filter { it != InstantiationType.NONE }
                                   .toTypedArray()).apply { selectedItem = InstantiationType.SHARED }
  private val perUser = ComboBox(InstantiationType.values.toTypedArray()).apply { selectedItem = InstantiationType.SHARED }

  private val setPermission = JBCheckBox(ZepMessagesBundle.message("repository.settings.options.permission"))
  private val owners = JBTextField(15)
  private val ownersBlock: MigBlock

  private val isExistingProcess = JBCheckBox(ZepMessagesBundle.message("repository.settings.options.isExistingProcess"))
  private val existingProcessHost = JBTextField(15)
  private val existingProcessPort = JBTextField(8)
  private val existingProcessBlock: MigBlock

  private val propertiesModel = DefaultTableModel().apply {
    addColumn(ZepMessagesBundle.message("interpreter.settings.properties.name"))
    addColumn(ZepMessagesBundle.message("interpreter.settings.properties.value"))
    addColumn(ZepMessagesBundle.message("interpreter.settings.properties.type"))
    if (showPropertiesDetails) {
      addColumn(ZepMessagesBundle.message("interpreter.settings.properties.description"))
    }
  }

  private val propertiesTable = JBTable(propertiesModel).apply {
    tableHeader.reorderingAllowed = false
    columnModel.getColumn(2).cellEditor = ComboBoxTableCellEditor.INSTANCE
    columnModel.getColumn(1).cellRenderer = InterpreterPropertiesRenderer()
    columnModel.getColumn(1).cellEditor = InterpreterPropertiesRenderer.instance
  }

  private val dependenciesModel = DefaultTableModel().apply {
    addColumn(ZepMessagesBundle.message("interpreter.settings.dependencies.artifact"))
    addColumn(ZepMessagesBundle.message("interpreter.settings.dependencies.exclude"))
  }

  private val dependenciesTable = JBTable(dependenciesModel).apply {
    tableHeader.reorderingAllowed = false
  }

  private val panel: MigPanel

  private val emptyPanel = JBPanelWithEmptyText().withEmptyText(ZepMessagesBundle.message("interpreter.settings.empty.text"))

  private val componentsWithValidation = listOf<JComponent>(interpreterName, interpreterGroup, existingProcessPort)

  private fun adjustInstantiationType(from: ComboBox<String>,
                                      to: ComboBox<String>,
                                      suitableInstantiationTypes: Map<String, List<String>>) {
    val perNoteType = (from.selectedItem as? String) ?: InstantiationType.NONE
    val perUserType = (to.selectedItem as? String) ?: InstantiationType.NONE

    val suitableTypes = suitableInstantiationTypes[perNoteType]
    if (suitableTypes != null && !suitableTypes.contains(perUserType)) {
      to.selectedItem = suitableTypes.first()
    }
  }

  init {
    if (mode == Mode.EDIT) {
      interpreterName.isEditable = false
    }

    existingProcessPort.withPortValidator(this)

    perNote.addItemListener {
      adjustInstantiationType(perNote, perUser, suitablePerNoteTypes)
    }

    perUser.addItemListener {
      adjustInstantiationType(perUser, perNote, suitablePerUserTypes)
    }

    if (mode == Mode.CREATION) {
      updateAvailableInterpreterTemplates(manager.getInterpreterTemplates())
    }

    interpreterGroup.addActionListener {
      val template = interpreterGroup.selectedItem as? InterpreterTemplate ?: return@addActionListener

      interpreterGroup.removeItem(null)

      propertiesModel.rowCount = 0
      template.properties.forEach { property ->
        propertiesModel.addRow(arrayOf(property.name, property.defaultValue, property.type, property.description))
      }

      MaterialTableUtils.fitColumnsWidth(propertiesTable)
    }

    panel = MigPanel().apply {

      val optionsPanel = MigPanel().apply {

        val namePanel = JPanel(FlowLayout(FlowLayout.LEFT))

        namePanel.add(JLabel(ZepMessagesBundle.message("interpreter.settings.field.name")))
        if (mode == Mode.CREATION) {
          namePanel.add(interpreterName)
        }
        else {
          namePanel.add(interpreterName)
          namePanel.add(interpreterStatus)
        }

        add(namePanel, UiUtil.spanXWrap)

        if (mode == Mode.CREATION) {
          interpreterGroupBlock = MigBlock(this).apply {
            shortRow(ZepMessagesBundle.message("interpreter.settings.field.group"), interpreterGroup)
          }
        }

        if (mode == Mode.EDIT) {
          add(interpreterGroups, CC().span().grow().wrap())
        }

        val instantiationPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
          add(JLabel(ZepMessagesBundle.message("repository.settings.options.instantiationPerNote")))
          add(perNote)
          add(JLabel(ZepMessagesBundle.message("repository.settings.options.instantiationPerUser")))
          add(perUser)
        }
        block(instantiationPanel)

        add(setPermission, UiUtil.spanXWrap)
        ownersBlock = MigBlock(this).apply {
          row(ZepMessagesBundle.message("repository.settings.options.owners"), owners)
        }
        ownersBlock.isVisible = false

        setPermission.addItemListener {
          owners.isEnabled = setPermission.isSelected
          ownersBlock.isVisible = setPermission.isSelected
        }

        add(isExistingProcess, UiUtil.spanXWrap)
        existingProcessBlock = MigBlock(this).apply {
          add(JLabel(ZepMessagesBundle.message("repository.settings.options.host")))
          add(existingProcessHost, UiUtil.pushXGrowX)
          add(JLabel(ZepMessagesBundle.message("repository.settings.options.port")))
          add(existingProcessPort, UiUtil.wrap)
        }
        existingProcessBlock.isVisible = false

        isExistingProcess.addItemListener {
          existingProcessHost.isEnabled = isExistingProcess.isSelected
          existingProcessPort.isEnabled = isExistingProcess.isSelected
          existingProcessBlock.isVisible = isExistingProcess.isSelected
        }
      }

      add(optionsPanel, CC().span().pushX().growX().wrap())

      val propertiesPanel = JPanel(BorderLayout()).apply {
        add(JLabel(ZepMessagesBundle.message("interpreter.settings.properties.title")), BorderLayout.NORTH)
        add(TableUiUtils.decorateTable(propertiesTable, propertiesModel) { arrayOf("", "", InterpreterPropertyType.string, "") },
            BorderLayout.CENTER)
      }

      val dependencyPanel = JPanel(BorderLayout()).apply {
        border = BorderFactory.createEmptyBorder(10, 0, 0, 0)
        add(JLabel(ZepMessagesBundle.message("interpreter.settings.dependencies.title")), BorderLayout.NORTH)
        add(TableUiUtils.decorateTable(dependenciesTable, dependenciesModel) { arrayOf("", "") }, BorderLayout.CENTER)
      }

      val onePixelSplitter = OnePixelSplitter(true, SPLITTER_PROPORTION_KEY, 0.6f).apply {
        firstComponent = propertiesPanel
        secondComponent = dependencyPanel
      }

      add(onePixelSplitter, CC().span().grow().push())
    }

    setInterpreterGroupValidator()
    setNameValidator()

    add(emptyPanel, BorderLayout.CENTER)

    minimumSize = Dimension(JBUIScale.scale(250), minimumSize.height)
    border = BorderFactory.createEmptyBorder(0, 10, 0, 10)
  }

  override fun dispose() = Unit

  fun updateAvailableInterpreterTemplates(interpreters: List<InterpreterTemplate>) {
    interpreterGroup.removeAllItems()
    interpreterGroup.addItem(null)
    val sortedTemplates = interpreters.sortedBy { it.group }
    sortedTemplates.forEach { interpreterGroup.addItem(it) }
  }

  private fun setNameValidator() {
    ComponentValidator(this)
      .withValidator(Supplier {
        if (interpreterName.text.isBlank()) {
          ValidationInfo(ZepMessagesBundle.message("interpreter.settings.validation.name.empty"), interpreterName)
        }
        else if (mode == Mode.CREATION) {
          if (manager.getInterpretersSettings().find { it.name == interpreterName.text } != null) {
            ValidationInfo(ZepMessagesBundle.message("interpreter.settings.validation.name.existing", interpreterName.text),
                           interpreterName)
          }
          else if (!INTERPRETER_NAME_PATTERN.matcher(interpreterName.text).find()) {
            ValidationInfo(ZepMessagesBundle.message("interpreter.settings.validation.name.invalid"), interpreterName)
          }
          else {
            null
          }
        }
        else {
          null
        }
      })
      .andRegisterOnDocumentListener(interpreterName)
      .installOn(interpreterName)
  }

  private fun setInterpreterGroupValidator() {
    ComponentValidator(this)
      .withValidator(Supplier {
        if (mode == Mode.CREATION && interpreterGroup.selectedItem == null) {
          ValidationInfo(ZepMessagesBundle.message("interpreter.settings.validation.group"), interpreterGroup)
        }
        else {
          null
        }
      })
      .installOn(interpreterGroup)

    interpreterGroup.addItemListener {
      ComponentValidator.getInstance(interpreterGroup).ifPresent {
        if (it.validationInfo != null) {
          it.revalidate()
        }
      }
    }
  }

  private fun clearDetails() {
    interpreterId.text = ""
    interpreterName.text = ""
    interpreterGroup.selectedItem = null
    interpreterErrorReason.text = ""
    interpreterStatus.status = InterpreterStatus.NOT_CREATED
    interpreterGroups.removeAll()

    propertiesModel.rowCount = 0
    dependenciesModel.rowCount = 0

    perNote.selectedItem = InstantiationType.SHARED
    perUser.selectedItem = InstantiationType.SHARED
    setPermission.isSelected = false
    owners.text = ""
    isExistingProcess.isSelected = false
    existingProcessHost.text = ""
    existingProcessPort.text = ""
  }


  fun getSettings(): InterpreterSettings? {

    val invalidComponent = componentsWithValidation.getInvalidComponent()

    if (invalidComponent != null) {
      return null
    }

    val properties = mutableMapOf<String, InterpreterProperty>()
    for (i in 0 until propertiesModel.rowCount) {

      if (propertiesModel.getValueAt(i, 0).toString().isBlank() &&
          propertiesModel.getValueAt(i, 1).toString().isBlank() &&
          (showPropertiesDetails && propertiesModel.getValueAt(i, 3).toString().isBlank())) {
        continue
      }

      val propertyType = InterpreterPropertyType.entries.firstOrNull { it.name == propertiesModel.getValueAt(i, 2).toString() }
                         ?: InterpreterPropertyType.string

      val property = InterpreterProperty(name = propertiesModel.getValueAt(i, 0).toString(),
                                         value = propertiesModel.getValueAt(i, 1).toString(),
                                         type = propertyType,
                                         description = if (showPropertiesDetails) propertiesModel.getValueAt(i, 3).toString() else "")


      properties[property.name] = property
    }

    val dependencies = mutableListOf<ZepDependency>()
    for (i in 0 until dependenciesModel.rowCount) {

      if (dependenciesModel.getValueAt(i, 0).toString().isBlank() &&
          dependenciesModel.getValueAt(i, 1).toString().isBlank()) {
        continue
      }

      val exclusionsString = dependenciesModel.getValueAt(i, 1).toString()
      val exclusions = if (exclusionsString.isBlank()) emptyList() else exclusionsString.split(",").map { it.trim() }

      val dependency = ZepDependency(groupArtifactVersion = dependenciesModel.getValueAt(i, 0).toString(),
                                     exclusions = exclusions)
      dependencies.add(dependency)
    }

    val localCurrentSettings = currentSettings

    @Suppress("DEPRECATION")
    val options = InterpreterOption(perNote = (perNote.selectedItem as? String) ?: InstantiationType.NONE,
                                    perUser = (perUser.selectedItem as? String) ?: InstantiationType.NONE,

                                    setPermission = setPermission.isSelected,
                                    owners = if (owners.text.isBlank()) emptyList() else owners.text.split(";").map { it.trim() },

                                    isExistingProcess = isExistingProcess.isSelected,
                                    host = existingProcessHost.text.ifBlank { null },
                                    port = existingProcessPort.text.toIntOrNull() ?: -1,

                                    remote = localCurrentSettings?.option?.remote ?: true,
                                    isUserImpersonate = localCurrentSettings?.option?.isUserImpersonate ?: true)

    val selectedGroup = interpreterGroup.selectedItem as? InterpreterTemplate

    val interpreterGroup = if (mode == Mode.EDIT && localCurrentSettings != null) {
      localCurrentSettings.interpreterGroup
    }
    else if (selectedGroup != null) {
      listOf(InterpreterInfo(name = selectedGroup.group))
    }
    else {
      emptyList()
    }

    val interpreterGroupName = if (mode == Mode.EDIT && localCurrentSettings != null) {
      localCurrentSettings.group
    }
    else {
      selectedGroup?.group ?: ""
    }

    return InterpreterSettings(id = if (mode == Mode.CREATION) "" else interpreterId.text,
                               name = interpreterName.text,
                               group = interpreterGroupName,
                               dependencies = dependencies,
                               status = interpreterStatus.status,
                               properties = properties,
                               option = options,
                               interpreterGroup = interpreterGroup,
                               errorReason = interpreterErrorReason.text)
  }

  fun showDetails(settings: InterpreterSettings?) {

    currentSettings = settings

    removeAll()

    if (settings == null) {
      clearDetails()
      add(emptyPanel, BorderLayout.CENTER)
      revalidate()
      repaint()
      return
    }

    add(panel, BorderLayout.CENTER)

    interpreterId.text = settings.id
    interpreterName.text = settings.name

    if (mode == Mode.CREATION) {
      interpreterName.select(0, interpreterName.text.length)
    }

    interpreterGroupBlock?.let {
      it.isVisible = settings.status == InterpreterStatus.NOT_CREATED
      if (it.isVisible) {

        var found: InterpreterTemplate? = null
        for (i in 0 until interpreterGroup.itemCount) {
          val item = interpreterGroup.getItemAt(i)

          if (item != null && item.group == settings.group) {
            found = item
            break
          }
        }

        found?.let { interpreterGroup.selectedItem = found }
      }
    }

    interpreterErrorReason.text = settings.errorReason
    interpreterStatus.status = settings.status
    interpreterStatus.toolTipText = settings.errorReason

    interpreterGroups.removeAll()

    settings.interpreterGroup.forEachIndexed { i, it ->
      interpreterGroups.add(InterpreterInfoLabel(if (i == 0) "%${settings.name}" else "%${settings.name}.${it.name}", it))
    }

    interpreterGroups.revalidate()
    interpreterGroups.repaint()

    perNote.selectedItem = settings.option.perNote
    perUser.selectedItem = settings.option.perUser

    setPermission.isSelected = settings.option.setPermission
    owners.isEnabled = setPermission.isSelected
    owners.text = settings.option.owners.joinToString(";")

    isExistingProcess.isSelected = settings.option.isExistingProcess
    existingProcessHost.isEnabled = isExistingProcess.isSelected
    existingProcessHost.text = settings.option.host
    existingProcessPort.isEnabled = isExistingProcess.isSelected
    existingProcessPort.text = if (settings.option.port == -1) "" else settings.option.port.toString()

    propertiesModel.rowCount = 0
    settings.properties.forEach {
      propertiesModel.addRow(arrayOf(it.value.name, it.value.value, it.value.type, it.value.description))
    }

    if (showPropertiesDetails) {
      propertiesTable.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
    }

    val columnWidthName = MaterialTableUtils.getColumnWidth(propertiesTable, 0, ColumnWidthFittingStrategy.MAX_WIDTH_BASED)
    val columnWidthValue = MaterialTableUtils.getColumnWidth(propertiesTable, 1, ColumnWidthFittingStrategy.MAX_WIDTH_BASED)
    val columnWidthType = ComboBox(arrayOf(InterpreterPropertyType.textarea)).preferredSize.width

    propertiesTable.columnModel.getColumn(0).apply {
      preferredWidth = columnWidthName
      minWidth = columnWidthName
      maxWidth = columnWidthName
    }

    if (showPropertiesDetails) {
      propertiesTable.columnModel.getColumn(1).apply {
        preferredWidth = columnWidthValue
        maxWidth = columnWidthValue
      }
    }

    propertiesTable.columnModel.getColumn(2).apply {
      preferredWidth = columnWidthType
      maxWidth = columnWidthType
    }

    dependenciesModel.rowCount = 0
    settings.dependencies.forEach {
      dependenciesModel.addRow(arrayOf(it.groupArtifactVersion, it.exclusions.joinToString(", ")))
    }

    revalidate()
    repaint()
  }
}